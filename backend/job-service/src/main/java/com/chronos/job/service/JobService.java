package com.chronos.job.service;

import com.chronos.job.dto.*;
import com.chronos.job.entity.Job;
import com.chronos.job.entity.JobPriority;
import com.chronos.job.entity.JobStatus;
import com.chronos.job.exception.ResourceNotFoundException;
import com.chronos.job.repository.JobRepository;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.List;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public JobResponse createJob(CreateJobRequest request, UUID organizationId) {
        validateScheduleAndTimezone(request.getSchedule(), request.getTimezone());

        Instant nextRunAt = calculateNextRunAt(request.getSchedule(), request.getTimezone());

        JobPriority priority = request.getPriority() != null ? request.getPriority() : JobPriority.NORMAL;

        Job job = new Job(
                organizationId,
                request.getName(),
                request.getDescription(),
                JobStatus.ACTIVE,
                request.getSchedule(),
                request.getTimezone(),
                true,
                priority,
                request.getTimeoutSeconds(),
                request.getMaxRetries(),
                request.getRetryBackoffSeconds(),
                nextRunAt
        );

        Job saved = jobRepository.save(job);
        return JobResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getJobsForOrganization(UUID organizationId) {
        return jobRepository.findAllByOrganizationId(organizationId)
                .stream()
                .map(JobResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(UUID jobId, UUID organizationId) {
        Job job = jobRepository.findByIdAndOrganizationId(jobId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));
        return JobResponse.fromEntity(job);
    }

    @Transactional
    public JobResponse updateJob(UUID jobId, UpdateJobRequest request, UUID organizationId) {
        Job job = jobRepository.findByIdAndOrganizationId(jobId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));

        validateScheduleAndTimezone(request.getSchedule(), request.getTimezone());
        Instant nextRunAt = calculateNextRunAt(request.getSchedule(), request.getTimezone());

        job.setName(request.getName());
        job.setDescription(request.getDescription());
        job.setSchedule(request.getSchedule());
        job.setTimezone(request.getTimezone());
        job.setPriority(request.getPriority());
        job.setTimeoutSeconds(request.getTimeoutSeconds());
        job.setMaxRetries(request.getMaxRetries());
        job.setRetryBackoffSeconds(request.getRetryBackoffSeconds());
        job.setNextRunAt(nextRunAt);

        Job updated = jobRepository.save(job);
        return JobResponse.fromEntity(updated);
    }

    @Transactional
    public JobResponse updateJobStatus(UUID jobId, JobStatusUpdateRequest request, UUID organizationId) {
        Job job = jobRepository.findByIdAndOrganizationId(jobId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));

        job.setStatus(request.getStatus());
        job.setEnabled(request.getStatus() == JobStatus.ACTIVE);

        Job updated = jobRepository.save(job);
        return JobResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteJob(UUID jobId, UUID organizationId) {
        Job job = jobRepository.findByIdAndOrganizationId(jobId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));
        jobRepository.delete(job);
    }

    private void validateScheduleAndTimezone(String schedule, String timezone) {
        if (!CronExpression.isValidExpression(schedule)) {
            throw new IllegalArgumentException("Invalid cron expression: " + schedule);
        }
        try {
            ZoneId.of(timezone);
        } catch (ZoneRulesException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid timezone: " + timezone);
        }
    }

    private Instant calculateNextRunAt(String schedule, String timezone) {
        try {
            CronExpression cron = CronExpression.parse(schedule);
            ZonedDateTime next = cron.next(ZonedDateTime.now(ZoneId.of(timezone)));
            return next != null ? next.toInstant() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
