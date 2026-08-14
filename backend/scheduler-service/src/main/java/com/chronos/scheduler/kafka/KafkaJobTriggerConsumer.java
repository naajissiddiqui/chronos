package com.chronos.scheduler.kafka;

import com.chronos.scheduler.event.JobTriggeredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaJobTriggerConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaJobTriggerConsumer.class);

    @KafkaListener(
            topics = "${kafka.topics.job-triggered:job.triggered}",
            groupId = "${spring.kafka.consumer.group-id:chronos-scheduler-test-consumer}"
    )
    public void consume(JobTriggeredEvent event) {
        if (event == null) {
            logger.warn("Received null JobTriggeredEvent from Kafka");
            return;
        }

        logger.info("Received job.triggered event: eventId={}, jobId={}, organizationId={}, scheduledAt={}, triggeredAt={}, priority={}",
                event.getEventId(),
                event.getJobId(),
                event.getOrganizationId(),
                event.getScheduledAt(),
                event.getTriggeredAt(),
                event.getPriority());
    }
}
