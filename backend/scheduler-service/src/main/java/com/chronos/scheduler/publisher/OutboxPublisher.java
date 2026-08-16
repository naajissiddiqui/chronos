package com.chronos.scheduler.publisher;

import com.chronos.scheduler.entity.OutboxEvent;
import com.chronos.scheduler.event.JobTriggeredEvent;
import com.chronos.scheduler.kafka.KafkaJobTriggerProducer;
import com.chronos.scheduler.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxService outboxService;
    private final KafkaJobTriggerProducer kafkaJobTriggerProducer;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxService outboxService,
                           KafkaJobTriggerProducer kafkaJobTriggerProducer,
                           ObjectMapper objectMapper) {
        this.outboxService = outboxService;
        this.kafkaJobTriggerProducer = kafkaJobTriggerProducer;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.interval-ms:1000}")
    public int processPendingEvents() {
        try {
            // Clean up any stale PROCESSING events (e.g. from crashed publisher instances)
            outboxService.resetStaleEvents(30);

            List<OutboxEvent> pendingEvents = outboxService.getPendingEvents();
            if (pendingEvents.isEmpty()) {
                return 0;
            }

            int publishedCount = 0;
            for (OutboxEvent outboxEvent : pendingEvents) {
                boolean claimed = outboxService.claimEvent(outboxEvent.getId());
                if (!claimed) {
                    logger.debug("Skipping outbox eventId={} (claimed by another publisher instance)", outboxEvent.getId());
                    continue;
                }

                try {
                    JobTriggeredEvent event = objectMapper.readValue(outboxEvent.getPayload(), JobTriggeredEvent.class);
                    kafkaJobTriggerProducer.sendJobTriggeredSync(event);
                    outboxService.markPublished(outboxEvent.getId(), Instant.now());
                    publishedCount++;
                } catch (Exception e) {
                    logger.warn("Failed to publish outbox eventId={}: {}. Leaving event pending for retry.",
                            outboxEvent.getId(), e.getMessage());
                    outboxService.handlePublishFailure(outboxEvent.getId(), e.getMessage());
                }
            }
            return publishedCount;
        } catch (Exception e) {
            logger.error("Error in OutboxPublisher scheduled processing cycle: {}", e.getMessage(), e);
            return 0;
        }
    }
}
