package com.chronos.scheduler.service;

import com.chronos.scheduler.entity.OutboxEvent;
import com.chronos.scheduler.entity.OutboxStatus;
import com.chronos.scheduler.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxService {

    private static final Logger logger = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxRepository outboxRepository;

    public OutboxService(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> getPendingEvents() {
        return outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimEvent(UUID eventId) {
        Instant now = Instant.now();
        int updated = outboxRepository.updateStatusIfCurrentStatus(
                eventId,
                OutboxStatus.PENDING,
                OutboxStatus.PROCESSING,
                now
        );
        return updated > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID eventId, Instant publishedAt) {
        Instant now = Instant.now();
        outboxRepository.markAsPublished(eventId, publishedAt, now);
        logger.info("Outbox event marked as PUBLISHED: eventId={}", eventId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePublishFailure(UUID eventId, String errorMessage) {
        Instant now = Instant.now();
        outboxRepository.handlePublishFailure(eventId, errorMessage, now);
        logger.warn("Outbox event publication failed, reset to PENDING: eventId={}, error={}", eventId, errorMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int resetStaleEvents(int staleThresholdSeconds) {
        Instant now = Instant.now();
        Instant staleTime = now.minus(staleThresholdSeconds, ChronoUnit.SECONDS);
        int count = outboxRepository.resetStaleProcessingEvents(staleTime, now);
        if (count > 0) {
            logger.info("Reset {} stale PROCESSING outbox events back to PENDING", count);
        }
        return count;
    }
}
