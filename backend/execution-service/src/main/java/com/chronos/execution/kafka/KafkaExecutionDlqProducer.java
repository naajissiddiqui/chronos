package com.chronos.execution.kafka;

import com.chronos.execution.event.ExecutionDlqEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaExecutionDlqProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaExecutionDlqProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String dlqTopic;

    public KafkaExecutionDlqProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topics.execution-dlq:execution.dlq}") String dlqTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.dlqTopic = dlqTopic;
    }

    public void sendExecutionDlq(ExecutionDlqEvent event) {
        if (event == null || event.getExecutionId() == null) {
            logger.warn("Attempted to publish null or invalid ExecutionDlqEvent");
            return;
        }

        try {
            kafkaTemplate.send(dlqTopic, event.getExecutionId().toString(), event);
            logger.info("Published ExecutionDlqEvent to topic '{}': executionId={}, finalAttempt={}, reason='{}'",
                    dlqTopic, event.getExecutionId(), event.getFinalAttempt(), event.getReason());
        } catch (Exception e) {
            logger.error("Failed to publish ExecutionDlqEvent for executionId={}: {}", event.getExecutionId(), e.getMessage(), e);
        }
    }
}
