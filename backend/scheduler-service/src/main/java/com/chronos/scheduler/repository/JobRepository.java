package com.chronos.scheduler.repository;

import com.chronos.scheduler.entity.Job;
import com.chronos.scheduler.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByEnabledTrueAndStatusAndNextRunAtLessThanEqual(JobStatus status, Instant now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Job j SET j.nextRunAt = :newNextRunAt, j.updatedAt = :now " +
           "WHERE j.id = :jobId AND j.nextRunAt <= :referenceTime " +
           "AND j.enabled = true AND j.status = com.chronos.scheduler.entity.JobStatus.ACTIVE")
    int claimAndUpdateNextRunAt(
            @Param("jobId") UUID jobId,
            @Param("referenceTime") Instant referenceTime,
            @Param("newNextRunAt") Instant newNextRunAt,
            @Param("now") Instant now);
}
