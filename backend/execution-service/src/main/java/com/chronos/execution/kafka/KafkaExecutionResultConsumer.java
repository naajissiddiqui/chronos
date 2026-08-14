package com.chronos.execution.kafka;

import com.chronos.execution.event.ExecutionCompletedEvent;
import com.chronos.execution.event.ExecutionFailedEvent;
import com.chronos.execution.service.ExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaExecutionResultConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaExecutionResultConsumer.class);

    private final ExecutionService executionService;

    public KafkaExecutionResultConsumer(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @KafkaListener(
            topics = "${kafka.topics.execution-completed:execution.completed}",
            groupId = "${spring.kafka.consumer.group-id:chronos-execution-service}"
    )
    public void consumeCompleted(ExecutionCompletedEvent event) {
        if (event == null || event.getExecutionId() == null) {
            logger.warn("Received null or invalid ExecutionCompletedEvent from Kafka");
            return;
        }

        logger.info("Received execution.completed: executionId={}, workerId={}, status=SUCCEEDED",
                event.getExecutionId(), event.getWorkerId());

        try {
            executionService.handleExecutionCompleted(event);
        } catch (Exception e) {
            logger.error("Failed to process execution.completed event for executionId={}: {}",
                    event.getExecutionId(), e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.execution-failed:execution.failed}",
            groupId = "${spring.kafka.consumer.group-id:chronos-execution-service}"
    )
    public void consumeFailed(ExecutionFailedEvent event) {
        if (event == null || event.getExecutionId() == null) {
            logger.warn("Received null or invalid ExecutionFailedEvent from Kafka");
            return;
        }

        logger.info("Received execution.failed: executionId={}, workerId={}, error={}",
                event.getExecutionId(), event.getWorkerId(), event.getErrorMessage());

        try {
            executionService.handleExecutionFailed(event);
        } catch (Exception e) {
            logger.error("Failed to process execution.failed event for executionId={}: {}",
                    event.getExecutionId(), e.getMessage(), e);
            throw e;
        }
    }
}
