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
import java.util.UUID;

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

    @Scheduled(fixedDelayString = "${outbox.publisher.interval-ms:200}")
    public int processPendingEvents() {
        try {
            // Clean up any stale PROCESSING events (e.g. from crashed publisher instances)
            outboxService.resetStaleEvents(30);

            List<OutboxEvent> pendingEvents = outboxService.getPendingEvents();
            if (pendingEvents.isEmpty()) {
                return 0;
            }

            java.util.List<UUID> successfulIds = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
            
            pendingEvents.parallelStream().forEach(outboxEvent -> {
                boolean claimed = outboxService.claimEvent(outboxEvent.getId());
                if (claimed) {
                    try {
                        JobTriggeredEvent event = objectMapper.readValue(outboxEvent.getPayload(), JobTriggeredEvent.class);
                        kafkaJobTriggerProducer.sendJobTriggeredSync(event);
                        successfulIds.add(outboxEvent.getId());
                    } catch (Exception e) {
                        logger.warn("Failed to publish outbox eventId={}: {}. Leaving event pending for retry.",
                                outboxEvent.getId(), e.getMessage());
                        outboxService.handlePublishFailure(outboxEvent.getId(), e.getMessage());
                    }
                }
            });

            if (!successfulIds.isEmpty()) {
                outboxService.markBatchPublished(successfulIds, Instant.now());
            }

            return successfulIds.size();
        } catch (Exception e) {
            logger.error("Error in OutboxPublisher scheduled processing cycle: {}", e.getMessage(), e);
            return 0;
        }
    }
}
