package com.chronos.worker.kafka;

import com.chronos.worker.event.ExecutionCompletedEvent;
import com.chronos.worker.event.ExecutionFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaWorkerResultProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaWorkerResultProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String completedTopic;
    private final String failedTopic;

    public KafkaWorkerResultProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topics.execution-completed:execution.completed}") String completedTopic,
            @Value("${kafka.topics.execution-failed:execution.failed}") String failedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.completedTopic = completedTopic;
        this.failedTopic = failedTopic;
    }

    public void sendExecutionCompleted(ExecutionCompletedEvent event) {
        if (event == null || event.getExecutionId() == null) {
            logger.error("Cannot publish null completed event or event without executionId");
            return;
        }

        String key = event.getExecutionId().toString();
        logger.info("Publishing execution.completed event to topic '{}': executionId={}, workerId={}",
                completedTopic, event.getExecutionId(), event.getWorkerId());

        kafkaTemplate.send(completedTopic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("Failed to publish execution.completed event for executionId={}: {}", event.getExecutionId(), ex.getMessage(), ex);
            } else {
                logger.info("Execution completed event published for executionId={} to topic '{}'", event.getExecutionId(), completedTopic);
            }
        });
    }

    public void sendExecutionFailed(ExecutionFailedEvent event) {
        if (event == null || event.getExecutionId() == null) {
            logger.error("Cannot publish null failed event or event without executionId");
            return;
        }

        String key = event.getExecutionId().toString();
        logger.info("Publishing execution.failed event to topic '{}': executionId={}, workerId={}, error={}",
                failedTopic, event.getExecutionId(), event.getWorkerId(), event.getErrorMessage());

        kafkaTemplate.send(failedTopic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("Failed to publish execution.failed event for executionId={}: {}", event.getExecutionId(), ex.getMessage(), ex);
            } else {
                logger.info("Execution failed event published for executionId={} to topic '{}'", event.getExecutionId(), failedTopic);
            }
        });
    }

    public String getCompletedTopic() {
        return completedTopic;
    }

    public String getFailedTopic() {
        return failedTopic;
    }
}
