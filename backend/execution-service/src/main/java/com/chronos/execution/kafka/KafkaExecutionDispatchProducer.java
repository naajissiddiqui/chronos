package com.chronos.execution.kafka;

import com.chronos.execution.event.ExecutionDispatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaExecutionDispatchProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaExecutionDispatchProducer.class);

    private final KafkaTemplate<String, ExecutionDispatchedEvent> kafkaTemplate;
    private final String topicName;

    public KafkaExecutionDispatchProducer(
            KafkaTemplate<String, ExecutionDispatchedEvent> kafkaTemplate,
            @Value("${kafka.topics.execution-dispatch:execution.dispatch}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void sendExecutionDispatched(ExecutionDispatchedEvent event) {
        if (event == null || event.getExecutionId() == null) {
            logger.error("Cannot publish null event or event without executionId");
            return;
        }

        String key = event.getExecutionId().toString();
        logger.info("Publishing execution.dispatch event to topic '{}': executionId={}, jobId={}, organizationId={}",
                topicName, event.getExecutionId(), event.getJobId(), event.getOrganizationId());

        kafkaTemplate.send(topicName, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("Failed to publish execution.dispatch event for executionId={}: {}", event.getExecutionId(), ex.getMessage(), ex);
            } else if (result != null && result.getRecordMetadata() != null) {
                logger.info("Successfully published execution.dispatch event for executionId={} to topic '{}' [partition={}, offset={}]",
                        event.getExecutionId(), topicName, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } else {
                logger.info("Published execution.dispatch event for executionId={} to topic '{}'", event.getExecutionId(), topicName);
            }
        });
    }

    public String getTopicName() {
        return topicName;
    }
}
