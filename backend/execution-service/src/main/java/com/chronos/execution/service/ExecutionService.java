package com.chronos.execution.service;

import com.chronos.execution.client.JobServiceClient;
import com.chronos.execution.dto.ExecutionResponse;
import com.chronos.execution.dto.JobRetryConfigDto;
import com.chronos.execution.entity.Execution;
import com.chronos.execution.entity.ExecutionStatus;
import com.chronos.execution.event.*;
import com.chronos.execution.exception.ResourceNotFoundException;
import com.chronos.execution.kafka.KafkaExecutionDispatchProducer;
import com.chronos.execution.kafka.KafkaExecutionDlqProducer;
import com.chronos.execution.kafka.KafkaExecutionRetryProducer;
import com.chronos.execution.repository.ExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);

    private final ExecutionRepository executionRepository;
    private final KafkaExecutionDispatchProducer kafkaExecutionDispatchProducer;
    private final KafkaExecutionRetryProducer kafkaExecutionRetryProducer;
    private final KafkaExecutionDlqProducer kafkaExecutionDlqProducer;
    private final JobServiceClient jobServiceClient;

    public ExecutionService(ExecutionRepository executionRepository,
                            KafkaExecutionDispatchProducer kafkaExecutionDispatchProducer,
                            KafkaExecutionRetryProducer kafkaExecutionRetryProducer,
                            KafkaExecutionDlqProducer kafkaExecutionDlqProducer,
                            JobServiceClient jobServiceClient) {
        this.executionRepository = executionRepository;
        this.kafkaExecutionDispatchProducer = kafkaExecutionDispatchProducer;
        this.kafkaExecutionRetryProducer = kafkaExecutionRetryProducer;
        this.kafkaExecutionDlqProducer = kafkaExecutionDlqProducer;
        this.jobServiceClient = jobServiceClient;
    }

    @Transactional
    public Optional<Execution> createExecutionFromEvent(JobTriggeredEvent event) {
        if (event == null || event.getEventId() == null || event.getJobId() == null || event.getOrganizationId() == null) {
            logger.error("Received invalid or malformed JobTriggeredEvent: {}", event);
            return Optional.empty();
        }

        // Idempotency check 1: Application-level check
        Optional<Execution> existing = executionRepository.findBySourceEventId(event.getEventId());
        if (existing.isPresent()) {
            logger.info("Duplicate event ignored: eventId={}, existingExecutionId={}", event.getEventId(), existing.get().getId());
            return existing;
        }

        Instant scheduledAt = event.getScheduledAt() != null ? event.getScheduledAt() : Instant.now();
        Execution execution = new Execution(
                event.getJobId(),
                event.getOrganizationId(),
                event.getEventId(),
                ExecutionStatus.PENDING,
                1,
                scheduledAt
        );

        try {
            Execution saved = executionRepository.saveAndFlush(execution);
            logger.info("Execution created: executionId={}, jobId={}, organizationId={}, status=PENDING",
                    saved.getId(), saved.getJobId(), saved.getOrganizationId());

            String taskType = "DEMO_REPORT";
            if (event.getPriority() != null && event.getPriority().toUpperCase().contains("FAIL")) {
                taskType = "DEMO_REPORT_FAIL";
            }

            // Dispatch execution to Kafka ONLY AFTER successful DB persistence
            ExecutionDispatchedEvent dispatchEvent = new ExecutionDispatchedEvent(
                    saved.getId(),
                    saved.getJobId(),
                    saved.getOrganizationId(),
                    saved.getAttempt(),
                    taskType,
                    "Demo report payload for jobId=" + saved.getJobId(),
                    Instant.now()
            );
            kafkaExecutionDispatchProducer.sendExecutionDispatched(dispatchEvent);

            return Optional.of(saved);
        } catch (DataIntegrityViolationException e) {
            // Idempotency check 2: Database uniqueness constraint fallback
            logger.info("Duplicate event ignored via database constraint: eventId={}", event.getEventId());
            return executionRepository.findBySourceEventId(event.getEventId());
        }
    }

    @Transactional
    public void handleExecutionCompleted(ExecutionCompletedEvent event) {
        if (event == null || event.getExecutionId() == null) {
            return;
        }

        Optional<Execution> opt = executionRepository.findById(event.getExecutionId());
        if (opt.isEmpty()) {
            logger.warn("Received completion event for non-existent executionId={}", event.getExecutionId());
            return;
        }

        Execution execution = opt.get();
        if (execution.getStatus() == ExecutionStatus.SUCCEEDED || execution.getStatus() == ExecutionStatus.FAILED) {
            logger.info("Execution already in terminal state ({}) for executionId={}, skipping duplicate completion event",
                    execution.getStatus(), execution.getId());
            return;
        }

        execution.setStatus(ExecutionStatus.SUCCEEDED);
        execution.setCompletedAt(event.getCompletedAt() != null ? event.getCompletedAt() : Instant.now());
        execution.setResult(event.getResult());
        execution.setWorkerId(event.getWorkerId());

        executionRepository.saveAndFlush(execution);
        logger.info("Execution completed: executionId={}, status=SUCCEEDED, workerId={}", execution.getId(), execution.getWorkerId());
    }

    @Transactional
    public void handleExecutionFailed(ExecutionFailedEvent event) {
        if (event == null || event.getExecutionId() == null) {
            return;
        }

        Optional<Execution> opt = executionRepository.findById(event.getExecutionId());
        if (opt.isEmpty()) {
            logger.warn("Received failure event for non-existent executionId={}", event.getExecutionId());
            return;
        }

        Execution execution = opt.get();
        if (execution.getStatus() == ExecutionStatus.SUCCEEDED ||
                execution.getStatus() == ExecutionStatus.FAILED ||
                execution.getStatus() == ExecutionStatus.RETRY_SCHEDULED) {
            logger.info("Execution already in terminal state or retry scheduled ({}) for executionId={}, skipping duplicate failure event",
                    execution.getStatus(), execution.getId());
            return;
        }

        int currentAttempt = event.getAttempt() != null ? event.getAttempt() : execution.getAttempt();
        logger.info("Execution failed: executionId={}, attempt={}", execution.getId(), currentAttempt);

        // Fetch Job retry configuration
        JobRetryConfigDto jobConfig = jobServiceClient.getJobRetryConfig(execution.getJobId(), execution.getOrganizationId());
        int maxRetries = jobConfig.getMaxRetries() != null ? jobConfig.getMaxRetries() : 3;
        int baseBackoffSeconds = jobConfig.getRetryBackoffSeconds() != null ? jobConfig.getRetryBackoffSeconds() : 10;

        boolean isRetryAvailable = currentAttempt <= maxRetries;

        if (isRetryAvailable) {
            // Calculate exponential backoff delay: delay = baseBackoffSeconds * 2^(currentAttempt - 1)
            long backoffDelaySeconds = (long) (baseBackoffSeconds * Math.pow(2, currentAttempt - 1));
            int nextAttempt = currentAttempt + 1;
            Instant retryAt = Instant.now().plusSeconds(backoffDelaySeconds);

            execution.setStatus(ExecutionStatus.RETRY_SCHEDULED);
            execution.setAttempt(nextAttempt);
            execution.setNextAttemptAt(retryAt);
            execution.setErrorMessage(event.getErrorMessage());
            execution.setWorkerId(event.getWorkerId());

            executionRepository.saveAndFlush(execution);
            logger.info("Retry scheduled: executionId={}, nextAttempt={}, retryAt={}", execution.getId(), nextAttempt, retryAt);

            String taskType = "DEMO_REPORT";
            if (event.getErrorMessage() != null && event.getErrorMessage().contains("DEMO_REPORT_FAIL")) {
                taskType = "DEMO_REPORT_FAIL";
            }

            ExecutionRetryEvent retryEvent = new ExecutionRetryEvent(
                    execution.getId(),
                    execution.getJobId(),
                    execution.getOrganizationId(),
                    nextAttempt,
                    retryAt,
                    event.getErrorMessage(),
                    Instant.now(),
                    taskType,
                    null
            );
            kafkaExecutionRetryProducer.sendExecutionRetry(retryEvent);
        } else {
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setCompletedAt(event.getFailedAt() != null ? event.getFailedAt() : Instant.now());
            execution.setErrorMessage(event.getErrorMessage());
            execution.setWorkerId(event.getWorkerId());

            executionRepository.saveAndFlush(execution);
            logger.info("Max retries exhausted: executionId={}", execution.getId());

            ExecutionDlqEvent dlqEvent = new ExecutionDlqEvent(
                    execution.getId(),
                    execution.getJobId(),
                    execution.getOrganizationId(),
                    currentAttempt,
                    "Max retries exhausted",
                    Instant.now(),
                    event.getWorkerId(),
                    event.getErrorMessage()
            );
            kafkaExecutionDlqProducer.sendExecutionDlq(dlqEvent);
            logger.info("Published to DLQ: executionId={}", execution.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<ExecutionResponse> getExecutionsForOrganization(UUID organizationId) {
        return executionRepository.findByOrganizationId(organizationId)
                .stream()
                .map(ExecutionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExecutionResponse getExecutionByIdAndOrganization(UUID id, UUID organizationId) {
        return executionRepository.findByIdAndOrganizationId(id, organizationId)
                .map(ExecutionResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<ExecutionResponse> getExecutionsByJobIdAndOrganization(UUID jobId, UUID organizationId) {
        return executionRepository.findByJobIdAndOrganizationId(jobId, organizationId)
                .stream()
                .map(ExecutionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
