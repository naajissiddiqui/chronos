package com.chronos.scheduler.service;

import com.chronos.scheduler.entity.Job;
import com.chronos.scheduler.entity.JobStatus;
import com.chronos.scheduler.event.JobTriggeredEvent;
import com.chronos.scheduler.kafka.KafkaJobTriggerProducer;
import com.chronos.scheduler.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JobSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(JobSchedulerService.class);

    private final JobRepository jobRepository;
    private final CronCalculationService cronCalculationService;
    private final KafkaJobTriggerProducer kafkaJobTriggerProducer;

    public JobSchedulerService(JobRepository jobRepository,
                               CronCalculationService cronCalculationService,
                               KafkaJobTriggerProducer kafkaJobTriggerProducer) {
        this.jobRepository = jobRepository;
        this.cronCalculationService = cronCalculationService;
        this.kafkaJobTriggerProducer = kafkaJobTriggerProducer;
    }

    public int processDueJobs() {
        Instant now = Instant.now();
        List<Job> dueJobs = jobRepository.findByEnabledTrueAndStatusAndNextRunAtLessThanEqual(JobStatus.ACTIVE, now);

        if (dueJobs.isEmpty()) {
            return 0;
        }

        int processedCount = 0;
        for (Job job : dueJobs) {
            boolean success = processSingleJob(job, now);
            if (success) {
                processedCount++;
            }
        }
        return processedCount;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processSingleJob(Job job, Instant referenceTime) {
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

                // Construct and publish Kafka event ONLY AFTER successful DB claim
                JobTriggeredEvent event = new JobTriggeredEvent(
                        UUID.randomUUID(),
                        job.getId(),
                        job.getOrganizationId(),
                        scheduledAt,
                        referenceTime,
                        job.getPriority()
                );

                kafkaJobTriggerProducer.sendJobTriggered(event);
                return true;
            } else {
                logger.warn("Job schedule update skipped (already updated or modified): jobId={}", job.getId());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error processing due job jobId={}: {}", job.getId(), e.getMessage(), e);
            return false;
        }
    }
}
