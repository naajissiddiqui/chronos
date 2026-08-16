package com.chronos.scheduler.service;

import com.chronos.scheduler.entity.Job;
import com.chronos.scheduler.entity.JobStatus;
import com.chronos.scheduler.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class JobSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(JobSchedulerService.class);

    private final JobRepository jobRepository;
    private final SingleJobProcessor singleJobProcessor;

    public JobSchedulerService(JobRepository jobRepository,
                               SingleJobProcessor singleJobProcessor) {
        this.jobRepository = jobRepository;
        this.singleJobProcessor = singleJobProcessor;
    }

    public int processDueJobs() {
        Instant now = Instant.now();
        List<Job> dueJobs = jobRepository.findByEnabledTrueAndStatusAndNextRunAtLessThanEqual(JobStatus.ACTIVE, now);

        if (dueJobs.isEmpty()) {
            return 0;
        }

        int processedCount = 0;
        for (Job job : dueJobs) {
            try {
                boolean success = singleJobProcessor.processSingleJob(job, now);
                if (success) {
                    processedCount++;
                }
            } catch (Exception e) {
                logger.error("Failed to process single job jobId={}: {}", job.getId(), e.getMessage());
            }
        }
        return processedCount;
    }

    public boolean processSingleJob(Job job, Instant referenceTime) {
        return singleJobProcessor.processSingleJob(job, referenceTime);
    }
}
