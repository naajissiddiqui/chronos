package com.chronos.scheduler.service;

import com.chronos.scheduler.entity.Job;
import com.chronos.scheduler.entity.JobPriority;
import com.chronos.scheduler.entity.JobStatus;
import com.chronos.scheduler.repository.JobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JobSchedulerServiceTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobSchedulerService jobSchedulerService;

    private UUID orgId;

    @BeforeAll
    static void initUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        orgId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        jobRepository.deleteAll();
    }

    private Job createJob(String name, JobStatus status, boolean enabled, String schedule, String timezone, Instant nextRunAt) {
        Job job = new Job();
        job.setOrganizationId(orgId);
        job.setName(name);
        job.setDescription("Test job " + name);
        job.setStatus(status);
        job.setEnabled(enabled);
        job.setSchedule(schedule);
        job.setTimezone(timezone);
        job.setPriority(JobPriority.NORMAL);
        job.setTimeoutSeconds(60);
        job.setMaxRetries(3);
        job.setRetryBackoffSeconds(10);
        job.setNextRunAt(nextRunAt);
        return jobRepository.saveAndFlush(job);
    }

    @Test
    void testActiveEnabledDueJobIsDetectedAndUpdated() {
        Instant past = Instant.now().minus(5, ChronoUnit.MINUTES);
        Job job = createJob("Due Job", JobStatus.ACTIVE, true, "*/5 * * * *", "UTC", past);

        int processed = jobSchedulerService.processDueJobs();

        assertEquals(1, processed);
        Job updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertTrue(updatedJob.getNextRunAt().isAfter(Instant.now()), "Updated nextRunAt must be in the future");
    }

    @Test
    void testFutureJobIsIgnored() {
        Instant future = Instant.now().plus(10, ChronoUnit.MINUTES);
        Job job = createJob("Future Job", JobStatus.ACTIVE, true, "*/5 * * * *", "UTC", future);

        int processed = jobSchedulerService.processDueJobs();

        assertEquals(0, processed);
        Job updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(future.truncatedTo(ChronoUnit.MILLIS), updatedJob.getNextRunAt().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void testDisabledJobIsIgnored() {
        Instant past = Instant.now().minus(5, ChronoUnit.MINUTES);
        Job job = createJob("Disabled Job", JobStatus.ACTIVE, false, "*/5 * * * *", "UTC", past);

        int processed = jobSchedulerService.processDueJobs();

        assertEquals(0, processed);
        Job updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(past.truncatedTo(ChronoUnit.MILLIS), updatedJob.getNextRunAt().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void testPausedJobIsIgnored() {
        Instant past = Instant.now().minus(5, ChronoUnit.MINUTES);
        Job job = createJob("Paused Job", JobStatus.PAUSED, true, "*/5 * * * *", "UTC", past);

        int processed = jobSchedulerService.processDueJobs();

        assertEquals(0, processed);
        Job updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(past.truncatedTo(ChronoUnit.MILLIS), updatedJob.getNextRunAt().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void testMultipleDueJobsAreHandled() {
        Instant past1 = Instant.now().minus(10, ChronoUnit.MINUTES);
        Instant past2 = Instant.now().minus(2, ChronoUnit.MINUTES);

        createJob("Job 1", JobStatus.ACTIVE, true, "*/5 * * * *", "UTC", past1);
        createJob("Job 2", JobStatus.ACTIVE, true, "0 * * * * *", "UTC", past2);

        int processed = jobSchedulerService.processDueJobs();

        assertEquals(2, processed);

        List<Job> allJobs = jobRepository.findAll();
        for (Job j : allJobs) {
            assertTrue(j.getNextRunAt().isAfter(Instant.now()), "All due jobs should be updated to future nextRunAt");
        }
    }

    @Test
    void testMissedScheduleMovesToNextFutureOccurrence() {
        Instant missedPast = Instant.now().minus(2, ChronoUnit.DAYS);
        Job job = createJob("Missed Job", JobStatus.ACTIVE, true, "0 0 12 * * *", "UTC", missedPast);

        int processed = jobSchedulerService.processDueJobs();

        assertEquals(1, processed);
        Job updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertTrue(updatedJob.getNextRunAt().isAfter(Instant.now()), "Missed schedule must be updated to future occurrence");
    }

    @Test
    void testRepeatedPollingDoesNotReProcessSameJob() {
        Instant past = Instant.now().minus(5, ChronoUnit.MINUTES);
        Job job = createJob("Idempotent Job", JobStatus.ACTIVE, true, "0 0 12 * * *", "UTC", past);

        // First poll processes the job
        int processedFirst = jobSchedulerService.processDueJobs();
        assertEquals(1, processedFirst);

        // Immediate second poll should find 0 due jobs because nextRunAt is now in the future
        int processedSecond = jobSchedulerService.processDueJobs();
        assertEquals(0, processedSecond);
    }

    @Test
    void testAtomicUpdateFailsIfOldNextRunAtMismatched() {
        Instant past = Instant.now().minus(5, ChronoUnit.MINUTES);
        Job job = createJob("Atomic Test Job", JobStatus.ACTIVE, true, "*/5 * * * *", "UTC", past);

        Instant earlierReferenceTime = Instant.now().minus(10, ChronoUnit.MINUTES);
        Instant newNextRunAt = Instant.now().plus(5, ChronoUnit.MINUTES);

        int updatedCount = jobRepository.claimAndUpdateNextRunAt(job.getId(), earlierReferenceTime, newNextRunAt, Instant.now());

        assertEquals(0, updatedCount, "Update should fail (0 rows updated) if job nextRunAt is after referenceTime");
    }
}
