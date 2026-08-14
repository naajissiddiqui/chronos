package com.chronos.execution.kafka;

import com.chronos.execution.event.JobTriggeredEvent;
import com.chronos.execution.service.ExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaJobTriggerConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaJobTriggerConsumer.class);

    private final ExecutionService executionService;

    public KafkaJobTriggerConsumer(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @KafkaListener(
            topics = "${kafka.topics.job-triggered:job.triggered}",
            groupId = "${spring.kafka.consumer.group-id:chronos-execution-service}"
    )
    public void consume(JobTriggeredEvent event) {
        if (event == null) {
            logger.warn("Received null JobTriggeredEvent from Kafka topic");
            return;
        }

        logger.info("Received job.triggered: eventId={}, jobId={}", event.getEventId(), event.getJobId());

        try {
            executionService.createExecutionFromEvent(event);
        } catch (Exception e) {
            logger.error("Failed to process job.triggered event eventId={}, jobId={}: {}",
                    event.getEventId(), event.getJobId(), e.getMessage(), e);
            throw e; // Rethrow to allow Kafka offset ACK suppression and redelivery
        }
    }
}
