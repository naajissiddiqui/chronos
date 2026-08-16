package com.chronos.scheduler.kafka;

import com.chronos.scheduler.event.JobTriggeredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
        try {
            sendJobTriggeredSync(event);
        } catch (Exception e) {
            logger.error("Failed to publish job.triggered event for jobId={}: {}", 
                    event != null ? event.getJobId() : null, e.getMessage());
        }
    }

    public SendResult<String, JobTriggeredEvent> sendJobTriggeredSync(JobTriggeredEvent event) throws Exception {
        if (event == null || event.getJobId() == null) {
            throw new IllegalArgumentException("Cannot publish null event or event without jobId");
        }

        String key = event.getJobId().toString();
        logger.info("Publishing job.triggered event synchronously to topic '{}': eventId={}, jobId={}, organizationId={}",
                topicName, event.getEventId(), event.getJobId(), event.getOrganizationId());

        CompletableFuture<SendResult<String, JobTriggeredEvent>> future = kafkaTemplate.send(topicName, key, event);
        SendResult<String, JobTriggeredEvent> result = future.get(5, TimeUnit.SECONDS);

        if (result != null && result.getRecordMetadata() != null) {
            logger.info("Successfully published job.triggered event for jobId={} to topic '{}' [partition={}, offset={}]",
                    event.getJobId(), topicName, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        } else {
            logger.info("Published job.triggered event for jobId={} to topic '{}'", event.getJobId(), topicName);
        }

        return result;
    }

    public String getTopicName() {
        return topicName;
    }
}
