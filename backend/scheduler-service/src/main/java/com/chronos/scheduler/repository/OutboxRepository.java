package com.chronos.scheduler.repository;

import com.chronos.scheduler.entity.OutboxEvent;
import com.chronos.scheduler.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop10ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    List<OutboxEvent> findByStatus(OutboxStatus status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE OutboxEvent o SET o.status = :newStatus, o.updatedAt = :now " +
           "WHERE o.id = :id AND o.status = :expectedStatus")
    int updateStatusIfCurrentStatus(
            @Param("id") UUID id,
            @Param("expectedStatus") OutboxStatus expectedStatus,
            @Param("newStatus") OutboxStatus newStatus,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE OutboxEvent o SET o.status = com.chronos.scheduler.entity.OutboxStatus.PUBLISHED, " +
           "o.publishedAt = :publishedAt, o.updatedAt = :now WHERE o.id = :id")
    int markAsPublished(
            @Param("id") UUID id,
            @Param("publishedAt") Instant publishedAt,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE OutboxEvent o SET o.status = com.chronos.scheduler.entity.OutboxStatus.PENDING, " +
           "o.retryCount = o.retryCount + 1, o.lastError = :lastError, o.updatedAt = :now WHERE o.id = :id")
    int handlePublishFailure(
            @Param("id") UUID id,
            @Param("lastError") String lastError,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE OutboxEvent o SET o.status = com.chronos.scheduler.entity.OutboxStatus.PENDING, " +
           "o.updatedAt = :now WHERE o.status = com.chronos.scheduler.entity.OutboxStatus.PROCESSING " +
           "AND o.updatedAt < :staleTime")
    int resetStaleProcessingEvents(
            @Param("staleTime") Instant staleTime,
            @Param("now") Instant now);
}
