package com.chronos.notification.kafka;

import com.chronos.notification.event.ExecutionCompletedEvent;
import com.chronos.notification.event.ExecutionFailedEvent;
import com.chronos.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for execution outcome events.
 *
 * <p>Consumes both {@code execution.completed} and {@code execution.failed}
 * using consumer group {@code chronos-notification-service}, which is independent
 * from the Execution Service's consumer group so both services receive every event.
 *
 * <p>Messages are NOT acknowledged before the notification has been safely
 * persisted: if the service call throws, the exception propagates up and
 * Kafka does not commit the offset, triggering redelivery.
 */
@Component
public class KafkaNotificationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaNotificationConsumer.class);

    private final NotificationService notificationService;

    public KafkaNotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${kafka.topics.execution-completed:execution.completed}",
            groupId = "${spring.kafka.consumer.group-id:chronos-notification-service}"
    )
    public void consumeCompleted(ExecutionCompletedEvent event) {
        if (event == null || event.getExecutionId() == null) {
            logger.warn("Received null or invalid ExecutionCompletedEvent from Kafka — dropping message");
            return;
        }

        logger.info("Received execution.completed: executionId={}, organizationId={}, workerId={}",
                event.getExecutionId(), event.getOrganizationId(), event.getWorkerId());

        try {
            notificationService.processExecutionCompleted(event);
        } catch (Exception e) {
            logger.error("Failed to process execution.completed for executionId={}: {}",
                    event.getExecutionId(), e.getMessage(), e);
            // Re-throw so Kafka does not commit the offset — enables redelivery
            throw e;
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.execution-failed:execution.failed}",
            groupId = "${spring.kafka.consumer.group-id:chronos-notification-service}"
    )
    public void consumeFailed(ExecutionFailedEvent event) {
        if (event == null || event.getExecutionId() == null) {
            logger.warn("Received null or invalid ExecutionFailedEvent from Kafka — dropping message");
            return;
        }

        logger.info("Received execution.failed: executionId={}, organizationId={}, error={}",
                event.getExecutionId(), event.getOrganizationId(), event.getErrorMessage());

        try {
            notificationService.processExecutionFailed(event);
        } catch (Exception e) {
            logger.error("Failed to process execution.failed for executionId={}: {}",
                    event.getExecutionId(), e.getMessage(), e);
            // Re-throw so Kafka does not commit the offset — enables redelivery
            throw e;
        }
    }
}
