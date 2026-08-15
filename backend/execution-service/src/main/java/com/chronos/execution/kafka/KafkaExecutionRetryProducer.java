package com.chronos.execution.kafka;

import com.chronos.execution.event.ExecutionRetryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaExecutionRetryProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaExecutionRetryProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String retryTopic;

    public KafkaExecutionRetryProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topics.execution-retry:execution.retry}") String retryTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.retryTopic = retryTopic;
    }

    public void sendExecutionRetry(ExecutionRetryEvent event) {
        if (event == null || event.getExecutionId() == null) {
            logger.warn("Attempted to publish null or invalid ExecutionRetryEvent");
            return;
        }

        try {
            kafkaTemplate.send(retryTopic, event.getExecutionId().toString(), event);
            logger.info("Published ExecutionRetryEvent to topic '{}': executionId={}, attempt={}, nextAttemptAt={}",
                    retryTopic, event.getExecutionId(), event.getAttempt(), event.getNextAttemptAt());
        } catch (Exception e) {
            logger.error("Failed to publish ExecutionRetryEvent for executionId={}: {}", event.getExecutionId(), e.getMessage(), e);
        }
    }
}
