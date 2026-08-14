package com.chronos.scheduler.kafka;

import com.chronos.scheduler.event.JobTriggeredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaJobTriggerProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaJobTriggerProducer.class);

    private final KafkaTemplate<String, JobTriggeredEvent> kafkaTemplate;
    private final String topicName;

    public KafkaJobTriggerProducer(
            KafkaTemplate<String, JobTriggeredEvent> kafkaTemplate,
            @Value("${kafka.topics.job-triggered:job.triggered}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void sendJobTriggered(JobTriggeredEvent event) {
        if (event == null || event.getJobId() == null) {
            logger.error("Cannot publish null event or event without jobId");
            return;
        }

        String key = event.getJobId().toString();
        logger.info("Publishing job.triggered event to topic '{}': eventId={}, jobId={}, organizationId={}",
                topicName, event.getEventId(), event.getJobId(), event.getOrganizationId());

        kafkaTemplate.send(topicName, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("Failed to publish job.triggered event for jobId={}: {}", event.getJobId(), ex.getMessage(), ex);
            } else if (result != null && result.getRecordMetadata() != null) {
                logger.info("Successfully published job.triggered event for jobId={} to topic '{}' [partition={}, offset={}]",
                        event.getJobId(), topicName, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } else {
                logger.info("Published job.triggered event for jobId={} to topic '{}'", event.getJobId(), topicName);
            }
        });
    }

    public String getTopicName() {
        return topicName;
    }
}
