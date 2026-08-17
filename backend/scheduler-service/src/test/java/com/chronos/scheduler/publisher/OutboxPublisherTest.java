package com.chronos.scheduler.publisher;

import com.chronos.scheduler.entity.*;
import com.chronos.scheduler.event.JobTriggeredEvent;
import com.chronos.scheduler.kafka.KafkaJobTriggerProducer;
import com.chronos.scheduler.repository.JobRepository;
import com.chronos.scheduler.repository.OutboxRepository;
import com.chronos.scheduler.scheduler.JobPollingScheduler;
import com.chronos.scheduler.service.JobSchedulerService;
import com.chronos.scheduler.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class OutboxPublisherTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private JobSchedulerService jobSchedulerService;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaJobTriggerProducer kafkaJobTriggerProducer;

    @MockBean
    private JobPollingScheduler jobPollingScheduler;



    private UUID orgId;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        jobRepository.deleteAll();
        orgId = UUID.randomUUID();
        reset(kafkaJobTriggerProducer);
    }

    @AfterEach
    void tearDown() {
        outboxRepository.deleteAll();
        jobRepository.deleteAll();
    }

    private Job createDueJob(String name) {
        Job job = new Job();
        job.setOrganizationId(orgId);
        job.setName(name);
        job.setDescription("Test job");
        job.setStatus(JobStatus.ACTIVE);
        job.setEnabled(true);
        job.setSchedule("*/5 * * * *");
        job.setTimezone("UTC");
        job.setPriority(JobPriority.HIGH);
        job.setTimeoutSeconds(60);
        job.setMaxRetries(3);
        job.setRetryBackoffSeconds(10);
        job.setNextRunAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        return jobRepository.saveAndFlush(job);
    }

    @Test
    void testSuccessfulJobClaimCreatesExactlyOneOutboxEvent() {
        Job job = createDueJob("Job Claim Test");

        int processed = jobSchedulerService.processDueJobs();
        assertEquals(1, processed);

        List<OutboxEvent> events = outboxRepository.findAll();
        assertEquals(1, events.size());
        OutboxEvent outboxEvent = events.get(0);
        assertEquals(job.getId(), outboxEvent.getAggregateId());
        assertEquals("JOB_TRIGGERED", outboxEvent.getEventType());
        assertEquals(OutboxStatus.PENDING, outboxEvent.getStatus());
        assertNull(outboxEvent.getPublishedAt());
    }

    @Test
    void testTransactionRollbackLeavesNoJobUpdateAndNoOutboxEvent() {
        Job job = createDueJob("Rollback Test Job");

        assertThrows(RuntimeException.class, () -> {
            // Force transactional rollback by passing null referenceTime
            jobSchedulerService.processSingleJob(job, null);
        });

        // Verify DB contains original job nextRunAt and zero outbox events
        Job reloadedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertTrue(reloadedJob.getNextRunAt().isBefore(Instant.now()));
        assertEquals(0, outboxRepository.count());
    }

    @Test
    void testKafkaAvailableEventPublishedAndOutboxMarkedPublished() throws Exception {
        Job job = createDueJob("Kafka Success Job");
        jobSchedulerService.processDueJobs();

        OutboxEvent pendingEvent = outboxRepository.findAll().get(0);
        assertEquals(OutboxStatus.PENDING, pendingEvent.getStatus());

        when(kafkaJobTriggerProducer.sendJobTriggeredSync(any(JobTriggeredEvent.class)))
                .thenReturn(mock(SendResult.class));

        int publishedCount = outboxPublisher.processPendingEvents();
        assertEquals(1, publishedCount);

        OutboxEvent publishedEvent = outboxRepository.findById(pendingEvent.getId()).orElseThrow();
        assertEquals(OutboxStatus.PUBLISHED, publishedEvent.getStatus());
        assertNotNull(publishedEvent.getPublishedAt());

        ArgumentCaptor<JobTriggeredEvent> captor = ArgumentCaptor.forClass(JobTriggeredEvent.class);
        verify(kafkaJobTriggerProducer, times(1)).sendJobTriggeredSync(captor.capture());
        assertEquals(job.getId(), captor.getValue().getJobId());
    }

    @Test
    void testKafkaUnavailableOutboxRemainsPending() throws Exception {
        createDueJob("Kafka Outage Job");
        jobSchedulerService.processDueJobs();

        when(kafkaJobTriggerProducer.sendJobTriggeredSync(any(JobTriggeredEvent.class)))
                .thenThrow(new RuntimeException("Kafka cluster unreachable"));

        int publishedCount = outboxPublisher.processPendingEvents();
        assertEquals(0, publishedCount);

        OutboxEvent failedEvent = outboxRepository.findAll().get(0);
        assertEquals(OutboxStatus.PENDING, failedEvent.getStatus());
        assertNull(failedEvent.getPublishedAt());
        assertEquals(1, failedEvent.getRetryCount());
        assertTrue(failedEvent.getLastError().contains("Kafka cluster unreachable"));
    }

    @Test
    void testKafkaComesBackPendingEventEventuallyPublished() throws Exception {
        createDueJob("Kafka Recovery Job");
        jobSchedulerService.processDueJobs();

        // 1st attempt: Kafka down
        when(kafkaJobTriggerProducer.sendJobTriggeredSync(any(JobTriggeredEvent.class)))
                .thenThrow(new RuntimeException("Kafka connection refused"));

        outboxPublisher.processPendingEvents();
        OutboxEvent event1 = outboxRepository.findAll().get(0);
        assertEquals(OutboxStatus.PENDING, event1.getStatus());

        // 2nd attempt: Kafka comes back up
        reset(kafkaJobTriggerProducer);
        when(kafkaJobTriggerProducer.sendJobTriggeredSync(any(JobTriggeredEvent.class)))
                .thenReturn(mock(SendResult.class));

        int publishedCount = outboxPublisher.processPendingEvents();
        assertEquals(1, publishedCount);

        OutboxEvent event2 = outboxRepository.findAll().get(0);
        assertEquals(OutboxStatus.PUBLISHED, event2.getStatus());
        assertNotNull(event2.getPublishedAt());
    }

    @Test
    void testPublisherRestartPendingEventsAreNotLost() throws Exception {
        createDueJob("Restart Resilience Job");
        jobSchedulerService.processDueJobs();

        // Simulate publisher stopping / restarting (new processing run on existing DB state)
        when(kafkaJobTriggerProducer.sendJobTriggeredSync(any(JobTriggeredEvent.class)))
                .thenReturn(mock(SendResult.class));

        int publishedCount = outboxPublisher.processPendingEvents();
        assertEquals(1, publishedCount);

        OutboxEvent event = outboxRepository.findAll().get(0);
        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
    }

    @Test
    void testDuplicatePublisherProcessingDoesNotCreateDuplicateJobEvents() throws Exception {
        createDueJob("Idempotency Test Job");
        jobSchedulerService.processDueJobs();

        when(kafkaJobTriggerProducer.sendJobTriggeredSync(any(JobTriggeredEvent.class)))
                .thenReturn(mock(SendResult.class));

        // First publisher run
        int firstRun = outboxPublisher.processPendingEvents();
        assertEquals(1, firstRun);

        // Immediate second publisher run (no new pending events)
        int secondRun = outboxPublisher.processPendingEvents();
        assertEquals(0, secondRun);

        // Verify Kafka producer only called once
        verify(kafkaJobTriggerProducer, times(1)).sendJobTriggeredSync(any(JobTriggeredEvent.class));
    }

    @Test
    void testConcurrentPublishersCannotProcessSameEventSimultaneously() throws Exception {
        createDueJob("Concurrent Claim Job");
        jobSchedulerService.processDueJobs();

        OutboxEvent pendingEvent = outboxRepository.findAll().get(0);

        // Simulate two threads attempting atomic claim simultaneously
        boolean firstClaim = outboxService.claimEvent(pendingEvent.getId());
        boolean secondClaim = outboxService.claimEvent(pendingEvent.getId());

        assertTrue(firstClaim, "First claim attempt must succeed");
        assertFalse(secondClaim, "Second concurrent claim attempt must fail");
    }
}
