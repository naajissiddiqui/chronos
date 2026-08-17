package com.chronos.scheduler.service;

import com.chronos.scheduler.entity.Job;
import com.chronos.scheduler.entity.OutboxEvent;
import com.chronos.scheduler.entity.OutboxStatus;
import com.chronos.scheduler.event.JobTriggeredEvent;
import com.chronos.scheduler.repository.JobRepository;
import com.chronos.scheduler.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SingleJobProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SingleJobProcessor.class);

    private final JobRepository jobRepository;
    private final OutboxRepository outboxRepository;
    private final CronCalculationService cronCalculationService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Autowired
    public SingleJobProcessor(JobRepository jobRepository,
                              OutboxRepository outboxRepository,
                              CronCalculationService cronCalculationService,
                              ObjectMapper objectMapper,
                              MeterRegistry meterRegistry) {
        this.jobRepository = jobRepository;
        this.outboxRepository = outboxRepository;
        this.cronCalculationService = cronCalculationService;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }


    public SingleJobProcessor(JobRepository jobRepository,
                              OutboxRepository outboxRepository,
                              CronCalculationService cronCalculationService,
                              ObjectMapper objectMapper) {
        this(jobRepository, outboxRepository, cronCalculationService, objectMapper, null);
    }

    @Transactional
    public boolean processSingleJob(Job job, Instant referenceTime) {
        if (referenceTime == null) {
            throw new IllegalArgumentException("referenceTime cannot be null");
        }
        try {
            logger.info("Scheduler detected due job: jobId={}, organizationId={}, previousNextRunAt={}",
                    job.getId(), job.getOrganizationId(), job.getNextRunAt());

            Instant scheduledAt = job.getNextRunAt();
            Instant newNextRunAt = cronCalculationService.calculateNextRunAt(
                    job.getSchedule(),
                    job.getTimezone(),
                    scheduledAt,
                    referenceTime
            );

            int updated = jobRepository.claimAndUpdateNextRunAt(
                    job.getId(),
                    referenceTime,
                    newNextRunAt,
                    Instant.now()
            );

            if (updated > 0) {
                logger.info("Scheduler successfully updated job schedule: jobId={}, organizationId={}, previousNextRunAt={}, nextRunAt={}",
                        job.getId(), job.getOrganizationId(), scheduledAt, newNextRunAt);

                UUID eventId = UUID.randomUUID();
                JobTriggeredEvent event = new JobTriggeredEvent(
                        eventId,
                        job.getId(),
                        job.getOrganizationId(),
                        scheduledAt,
                        referenceTime,
                        job.getPriority()
                );

                String payload = objectMapper.writeValueAsString(event);
                OutboxEvent outboxEvent = new OutboxEvent(
                        eventId,
                        "JOB_TRIGGERED",
                        job.getId(),
                        payload,
                        Instant.now(),
                        OutboxStatus.PENDING
                );

                outboxRepository.saveAndFlush(outboxEvent);
                logger.info("Transactional outbox event created in DB: eventId={}, jobId={}", eventId, job.getId());
                if (meterRegistry != null) {
                    Counter.builder("scheduler_jobs_triggered_total")
                            .description("Total jobs triggered by scheduler")
                            .register(meterRegistry)
                            .increment();
                }
                return true;

            } else {
                logger.warn("Job schedule update skipped (already updated or modified): jobId={}", job.getId());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error processing due job jobId={}: {}", job.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process due job jobId=" + job.getId(), e);
        }
    }
}
