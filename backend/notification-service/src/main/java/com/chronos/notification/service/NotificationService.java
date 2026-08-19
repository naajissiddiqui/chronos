package com.chronos.notification.service;

import com.chronos.notification.dto.NotificationResponse;
import com.chronos.notification.entity.Notification;
import com.chronos.notification.entity.NotificationStatus;
import com.chronos.notification.entity.NotificationType;
import com.chronos.notification.event.ExecutionCompletedEvent;
import com.chronos.notification.event.ExecutionFailedEvent;
import com.chronos.notification.exception.ResourceNotFoundException;
import com.chronos.notification.provider.NotificationProvider;
import com.chronos.notification.provider.NotificationRequest;
import com.chronos.notification.provider.NotificationResult;
import com.chronos.notification.repository.NotificationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationProvider notificationProvider;
    private final MeterRegistry meterRegistry;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationProvider notificationProvider,
                               MeterRegistry meterRegistry) {
        this.notificationRepository = notificationRepository;
        this.notificationProvider = notificationProvider;
        this.meterRegistry = meterRegistry;
    }

    // -------------------------------------------------------------------------
    // Kafka event handlers
    // -------------------------------------------------------------------------

    /**
     * Processes an execution.completed Kafka event.
     *
     * <p>Flow: idempotency check → persist PENDING → mark SENDING →
     * dispatch via provider → mark SENT or FAILED.
     *
     * <p>The full persistence happens before the Kafka message is acknowledged
     * (caller re-throws on failure so the listener does not commit the offset).
     */
    @Transactional
    public void processExecutionCompleted(ExecutionCompletedEvent event) {
        if (event == null || event.getExecutionId() == null || event.getOrganizationId() == null) {
            logger.warn("Received null or invalid ExecutionCompletedEvent — skipping");
            return;
        }

        UUID executionId = event.getExecutionId();
        NotificationType type = NotificationType.EXECUTION_SUCCEEDED;

        // ---- Application-level idempotency check ----
        if (notificationRepository.existsByExecutionIdAndType(executionId, type)) {
            logger.info("Duplicate execution.completed ignored: executionId={}, type={}", executionId, type);
            return;
        }

        String recipient = "org-" + event.getOrganizationId() + "@notifications.chronos.internal";
        String subject = "Execution succeeded: " + executionId;
        String message = buildSuccessMessage(event);

        Notification notification = new Notification(
                event.getOrganizationId(),
                executionId,
                event.getJobId(),
                type,
                recipient,
                subject,
                message
        );

        try {
            notification = notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException e) {
            // DB-level idempotency fallback (race-condition safety net)
            logger.info("Duplicate execution.completed blocked by DB constraint: executionId={}", executionId);
            return;
        }

        incrementCounter("notifications_created_total");
        logger.info("Notification created: id={}, executionId={}, type={}", notification.getId(), executionId, type);

        dispatchAndUpdateStatus(notification, event.getCompletedAt());
    }

    /**
     * Processes an execution.failed Kafka event.
     *
     * <p>Same idempotency + dispatch flow as
     * {@link #processExecutionCompleted(ExecutionCompletedEvent)}.
     */
    @Transactional
    public void processExecutionFailed(ExecutionFailedEvent event) {
        if (event == null || event.getExecutionId() == null || event.getOrganizationId() == null) {
            logger.warn("Received null or invalid ExecutionFailedEvent — skipping");
            return;
        }

        UUID executionId = event.getExecutionId();
        NotificationType type = NotificationType.EXECUTION_FAILED;

        // ---- Application-level idempotency check ----
        if (notificationRepository.existsByExecutionIdAndType(executionId, type)) {
            logger.info("Duplicate execution.failed ignored: executionId={}, type={}", executionId, type);
            return;
        }

        String recipient = "org-" + event.getOrganizationId() + "@notifications.chronos.internal";
        String subject = "Execution failed: " + executionId;
        String message = buildFailureMessage(event);

        Notification notification = new Notification(
                event.getOrganizationId(),
                executionId,
                event.getJobId(),
                type,
                recipient,
                subject,
                message
        );

        try {
            notification = notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException e) {
            // DB-level idempotency fallback (race-condition safety net)
            logger.info("Duplicate execution.failed blocked by DB constraint: executionId={}", executionId);
            return;
        }

        incrementCounter("notifications_created_total");
        logger.info("Notification created: id={}, executionId={}, type={}", notification.getId(), executionId, type);

        dispatchAndUpdateStatus(notification, event.getFailedAt());
    }

    // -------------------------------------------------------------------------
    // Read APIs
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForOrganization(UUID organizationId) {
        return notificationRepository.findByOrganizationId(organizationId)
                .stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotificationByIdAndOrganization(UUID id, UUID organizationId) {
        return notificationRepository.findByIdAndOrganizationId(id, organizationId)
                .map(NotificationResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForExecution(UUID executionId, UUID organizationId) {
        return notificationRepository.findByExecutionIdAndOrganizationId(executionId, organizationId)
                .stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Calls the injected provider, then updates the notification status to
     * SENT or FAILED based on the result. Timer is recorded around the
     * full delivery attempt.
     */
    private void dispatchAndUpdateStatus(Notification notification, Instant eventTimestamp) {
        Instant dispatchStart = Instant.now();

        notification.setStatus(NotificationStatus.SENDING);
        notificationRepository.saveAndFlush(notification);

        NotificationRequest request = new NotificationRequest(
                notification.getId(),
                notification.getOrganizationId(),
                notification.getExecutionId(),
                notification.getJobId(),
                notification.getType(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getMessage()
        );

        NotificationResult result;
        try {
            result = notificationProvider.send(request);
        } catch (Exception e) {
            result = NotificationResult.failure("Provider threw exception: " + e.getMessage());
            logger.error("Provider threw unexpected exception for notificationId={}: {}",
                    notification.getId(), e.getMessage(), e);
        }

        Instant dispatchEnd = Instant.now();
        recordDeliveryDuration(dispatchStart, dispatchEnd);

        if (result.isSuccess()) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(dispatchEnd);
            notificationRepository.saveAndFlush(notification);
            incrementCounter("notifications_sent_total");
            logger.info("Notification SENT: id={}, executionId={}, type={}",
                    notification.getId(), notification.getExecutionId(), notification.getType());
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(result.getErrorMessage());
            notificationRepository.saveAndFlush(notification);
            incrementCounter("notifications_failed_total");
            logger.warn("Notification FAILED: id={}, executionId={}, type={}, reason={}",
                    notification.getId(), notification.getExecutionId(), notification.getType(),
                    result.getErrorMessage());
        }
    }

    private String buildSuccessMessage(ExecutionCompletedEvent event) {
        return String.format(
                "Job execution succeeded.\n" +
                        "Execution ID : %s\n" +
                        "Job ID       : %s\n" +
                        "Organization : %s\n" +
                        "Worker       : %s\n" +
                        "Attempt      : %d\n" +
                        "Completed At : %s\n" +
                        "Result       : %s",
                event.getExecutionId(),
                event.getJobId(),
                event.getOrganizationId(),
                event.getWorkerId(),
                event.getAttempt() != null ? event.getAttempt() : 1,
                event.getCompletedAt() != null ? event.getCompletedAt() : Instant.now(),
                event.getResult() != null ? event.getResult() : "N/A"
        );
    }

    private String buildFailureMessage(ExecutionFailedEvent event) {
        return String.format(
                "Job execution failed.\n" +
                        "Execution ID  : %s\n" +
                        "Job ID        : %s\n" +
                        "Organization  : %s\n" +
                        "Worker        : %s\n" +
                        "Attempt       : %d\n" +
                        "Failed At     : %s\n" +
                        "Error Message : %s",
                event.getExecutionId(),
                event.getJobId(),
                event.getOrganizationId(),
                event.getWorkerId(),
                event.getAttempt() != null ? event.getAttempt() : 1,
                event.getFailedAt() != null ? event.getFailedAt() : Instant.now(),
                event.getErrorMessage() != null ? event.getErrorMessage() : "Unknown error"
        );
    }

    private void incrementCounter(String name) {
        if (meterRegistry != null) {
            Counter.builder(name)
                    .description("Notification service counter: " + name)
                    .register(meterRegistry)
                    .increment();
        }
    }

    private void recordDeliveryDuration(Instant start, Instant end) {
        if (meterRegistry != null) {
            long durationMillis = Duration.between(start, end).toMillis();
            if (durationMillis >= 0) {
                Timer.builder("notification_delivery_duration")
                        .description("Time taken to dispatch a notification through the provider")
                        .register(meterRegistry)
                        .record(durationMillis, TimeUnit.MILLISECONDS);
            }
        }
    }
}
