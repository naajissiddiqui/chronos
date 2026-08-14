package com.chronos.scheduler.service;

import com.chronos.scheduler.entity.Job;
import com.chronos.scheduler.entity.JobStatus;
import com.chronos.scheduler.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class JobSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(JobSchedulerService.class);

    private final JobRepository jobRepository;
    private final CronCalculationService cronCalculationService;

    public JobSchedulerService(JobRepository jobRepository, CronCalculationService cronCalculationService) {
        this.jobRepository = jobRepository;
        this.cronCalculationService = cronCalculationService;
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

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public boolean processSingleJob(Job job, Instant referenceTime) {
        try {
            logger.info("Scheduler detected due job: jobId={}, organizationId={}, previousNextRunAt={}",
                    job.getId(), job.getOrganizationId(), job.getNextRunAt());

            Instant newNextRunAt = cronCalculationService.calculateNextRunAt(
                    job.getSchedule(),
                    job.getTimezone(),
                    job.getNextRunAt(),
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
                        job.getId(), job.getOrganizationId(), job.getNextRunAt(), newNextRunAt);
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
