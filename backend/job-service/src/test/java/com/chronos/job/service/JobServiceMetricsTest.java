package com.chronos.job.service;

import com.chronos.job.dto.CreateJobRequest;
import com.chronos.job.dto.JobResponse;
import com.chronos.job.dto.JobStatusUpdateRequest;
import com.chronos.job.entity.Job;
import com.chronos.job.entity.JobPriority;
import com.chronos.job.entity.JobStatus;
import com.chronos.job.repository.JobRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceMetricsTest {

    @Mock
    private JobRepository jobRepository;

    private MeterRegistry meterRegistry;
    private JobService jobService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        jobService = new JobService(jobRepository, meterRegistry);
    }

    @Test
    void testCreateJobIncrementsCreatedCounter() {
        CreateJobRequest request = new CreateJobRequest();
        request.setName("Test Job");
        request.setSchedule("0 0 * * * ?");
        request.setTimezone("UTC");
        request.setPriority(JobPriority.HIGH);

        UUID orgId = UUID.randomUUID();
        Job mockSaved = new Job(orgId, request.getName(), "desc", JobStatus.ACTIVE, request.getSchedule(), request.getTimezone(), true, request.getPriority(), 60, 3, 10, null);

        when(jobRepository.save(any(Job.class))).thenReturn(mockSaved);

        JobResponse response = jobService.createJob(request, orgId);

        assertEquals("Test Job", response.getName());
        assertEquals(1.0, meterRegistry.find("jobs_created_total").counter().count());
    }

    @Test
    void testUpdateJobStatusIncrementsStatusChangesCounter() {
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Job mockJob = new Job(orgId, "Job1", "desc", JobStatus.ACTIVE, "0 0 * * * ?", "UTC", true, JobPriority.NORMAL, 60, 3, 10, null);

        when(jobRepository.findByIdAndOrganizationId(jobId, orgId)).thenReturn(Optional.of(mockJob));
        when(jobRepository.save(any(Job.class))).thenReturn(mockJob);

        JobStatusUpdateRequest request = new JobStatusUpdateRequest();
        request.setStatus(JobStatus.PAUSED);

        jobService.updateJobStatus(jobId, request, orgId);

        assertEquals(1.0, meterRegistry.find("jobs_status_changes_total").tag("status", "PAUSED").counter().count());
    }

    @Test
    void testDeleteJobIncrementsDeletedCounter() {
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Job mockJob = new Job(orgId, "Job1", "desc", JobStatus.ACTIVE, "0 0 * * * ?", "UTC", true, JobPriority.NORMAL, 60, 3, 10, null);

        when(jobRepository.findByIdAndOrganizationId(jobId, orgId)).thenReturn(Optional.of(mockJob));

        jobService.deleteJob(jobId, orgId);

        assertEquals(1.0, meterRegistry.find("jobs_deleted_total").counter().count());
    }
}
