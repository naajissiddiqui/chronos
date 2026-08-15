package com.chronos.execution.service;

import com.chronos.execution.client.JobServiceClient;
import com.chronos.execution.dto.JobRetryConfigDto;
import com.chronos.execution.entity.Execution;
import com.chronos.execution.entity.ExecutionStatus;
import com.chronos.execution.event.*;
import com.chronos.execution.kafka.KafkaExecutionDispatchProducer;
import com.chronos.execution.kafka.KafkaExecutionDlqProducer;
import com.chronos.execution.kafka.KafkaExecutionRetryProducer;
import com.chronos.execution.repository.ExecutionRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExecutionServiceTest {

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private ExecutionService executionService;

    @MockBean
    private KafkaExecutionDispatchProducer kafkaExecutionDispatchProducer;

    @MockBean
    private KafkaExecutionRetryProducer kafkaExecutionRetryProducer;

    @MockBean
    private KafkaExecutionDlqProducer kafkaExecutionDlqProducer;

    @MockBean
    private JobServiceClient jobServiceClient;

    @BeforeAll
    static void initUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setUp() {
        executionRepository.deleteAll();
        reset(kafkaExecutionDispatchProducer, kafkaExecutionRetryProducer, kafkaExecutionDlqProducer, jobServiceClient);
        when(jobServiceClient.getJobRetryConfig(any(), any())).thenReturn(new JobRetryConfigDto(3, 10));
    }

    @Test
    void testJobTriggeredEventCreatesPendingExecutionAndDispatchesEvent() {
        UUID eventId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Instant scheduledAt = Instant.now().minusSeconds(60);

        JobTriggeredEvent event = new JobTriggeredEvent(eventId, jobId, orgId, scheduledAt, Instant.now(), "HIGH");

        Optional<Execution> createdOpt = executionService.createExecutionFromEvent(event);

        assertTrue(createdOpt.isPresent());
        Execution execution = createdOpt.get();

        assertNotNull(execution.getId());
        assertEquals(jobId, execution.getJobId());
        assertEquals(orgId, execution.getOrganizationId());
        assertEquals(eventId, execution.getSourceEventId());
        assertEquals(ExecutionStatus.PENDING, execution.getStatus());
        assertEquals(1, execution.getAttempt());

        // Verify dispatch event published
        ArgumentCaptor<ExecutionDispatchedEvent> captor = ArgumentCaptor.forClass(ExecutionDispatchedEvent.class);
        verify(kafkaExecutionDispatchProducer, times(1)).sendExecutionDispatched(captor.capture());

        ExecutionDispatchedEvent dispatchEvent = captor.getValue();
        assertEquals(execution.getId(), dispatchEvent.getExecutionId());
        assertEquals(jobId, dispatchEvent.getJobId());
        assertEquals("DEMO_REPORT", dispatchEvent.getTaskType());
    }

    @Test
    void testHandleExecutionCompletedUpdatesStatusToSucceeded() {
        UUID eventId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        JobTriggeredEvent event = new JobTriggeredEvent(eventId, jobId, orgId, Instant.now(), Instant.now(), "HIGH");
        Execution execution = executionService.createExecutionFromEvent(event).orElseThrow();

        Instant completedAt = Instant.now();
        ExecutionCompletedEvent completedEvent = new ExecutionCompletedEvent(
                execution.getId(),
                jobId,
                orgId,
                "worker-local-1",
                1,
                completedAt,
                "Demo report generated successfully"
        );

        executionService.handleExecutionCompleted(completedEvent);

        Execution updated = executionRepository.findById(execution.getId()).orElseThrow();
        assertEquals(ExecutionStatus.SUCCEEDED, updated.getStatus());
        assertEquals("worker-local-1", updated.getWorkerId());
        assertEquals("Demo report generated successfully", updated.getResult());
        verify(kafkaExecutionRetryProducer, never()).sendExecutionRetry(any());
        verify(kafkaExecutionDlqProducer, never()).sendExecutionDlq(any());
    }

    @Test
    void testFirstExecutionFailureTriggersRetryAndCalculatesExponentialBackoff() {
        UUID eventId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        JobTriggeredEvent event = new JobTriggeredEvent(eventId, jobId, orgId, Instant.now(), Instant.now(), "NORMAL");
        Execution execution = executionService.createExecutionFromEvent(event).orElseThrow();

        ExecutionFailedEvent failedEvent = new ExecutionFailedEvent(
                execution.getId(),
                jobId,
                orgId,
                "worker-local-1",
                1,
                Instant.now(),
                "Error processing report"
        );

        executionService.handleExecutionFailed(failedEvent);

        Execution updated = executionRepository.findById(execution.getId()).orElseThrow();
        assertEquals(ExecutionStatus.RETRY_SCHEDULED, updated.getStatus());
        assertEquals(2, updated.getAttempt()); // Next attempt set to 2
        assertNotNull(updated.getNextAttemptAt());

        // Initial attempt 1 failed with base backoff 10s: delay = 10 * 2^0 = 10s
        long delaySeconds = Duration.between(Instant.now(), updated.getNextAttemptAt()).getSeconds();
        assertTrue(delaySeconds >= 8 && delaySeconds <= 12, "Backoff delay should be approx 10s, got " + delaySeconds);

        ArgumentCaptor<ExecutionRetryEvent> captor = ArgumentCaptor.forClass(ExecutionRetryEvent.class);
        verify(kafkaExecutionRetryProducer, times(1)).sendExecutionRetry(captor.capture());
        ExecutionRetryEvent retryEvent = captor.getValue();
        assertEquals(execution.getId(), retryEvent.getExecutionId());
        assertEquals(2, retryEvent.getAttempt());
    }

    @Test
    void testSecondExecutionFailureCalculatesExponentialBackoffCorrectly() {
        UUID eventId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        JobTriggeredEvent event = new JobTriggeredEvent(eventId, jobId, orgId, Instant.now(), Instant.now(), "NORMAL");
        Execution execution = executionService.createExecutionFromEvent(event).orElseThrow();

        // Simulate attempt 2 failing
        execution.setAttempt(2);
        execution.setStatus(ExecutionStatus.PENDING);
        executionRepository.saveAndFlush(execution);

        ExecutionFailedEvent failedEvent = new ExecutionFailedEvent(
                execution.getId(),
                jobId,
                orgId,
                "worker-local-1",
                2,
                Instant.now(),
                "Error on attempt 2"
        );

        executionService.handleExecutionFailed(failedEvent);

        Execution updated = executionRepository.findById(execution.getId()).orElseThrow();
        assertEquals(ExecutionStatus.RETRY_SCHEDULED, updated.getStatus());
        assertEquals(3, updated.getAttempt());

        // Attempt 2 failed with base backoff 10s: delay = 10 * 2^1 = 20s
        long delaySeconds = Duration.between(Instant.now(), updated.getNextAttemptAt()).getSeconds();
        assertTrue(delaySeconds >= 18 && delaySeconds <= 22, "Backoff delay for attempt 2 should be approx 20s, got " + delaySeconds);
    }

    @Test
    void testFailureAfterMaxRetriesExhaustedRoutesToDlq() {
        UUID eventId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        JobTriggeredEvent event = new JobTriggeredEvent(eventId, jobId, orgId, Instant.now(), Instant.now(), "NORMAL");
        Execution execution = executionService.createExecutionFromEvent(event).orElseThrow();

        // Simulate attempt 4 failing (where maxRetries = 3)
        execution.setAttempt(4);
        execution.setStatus(ExecutionStatus.PENDING);
        executionRepository.saveAndFlush(execution);

        ExecutionFailedEvent failedEvent = new ExecutionFailedEvent(
                execution.getId(),
                jobId,
                orgId,
                "worker-local-1",
                4,
                Instant.now(),
                "Final error on attempt 4"
        );

        executionService.handleExecutionFailed(failedEvent);

        Execution updated = executionRepository.findById(execution.getId()).orElseThrow();
        assertEquals(ExecutionStatus.FAILED, updated.getStatus());

        verify(kafkaExecutionRetryProducer, never()).sendExecutionRetry(any());
        ArgumentCaptor<ExecutionDlqEvent> captor = ArgumentCaptor.forClass(ExecutionDlqEvent.class);
        verify(kafkaExecutionDlqProducer, times(1)).sendExecutionDlq(captor.capture());

        ExecutionDlqEvent dlqEvent = captor.getValue();
        assertEquals(execution.getId(), dlqEvent.getExecutionId());
        assertEquals(4, dlqEvent.getFinalAttempt());
        assertEquals("worker-local-1", dlqEvent.getLastWorkerId());
    }

    @Test
    void testDuplicateFailureEventDoesNotScheduleDuplicateRetry() {
        UUID eventId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        JobTriggeredEvent event = new JobTriggeredEvent(eventId, jobId, orgId, Instant.now(), Instant.now(), "NORMAL");
        Execution execution = executionService.createExecutionFromEvent(event).orElseThrow();

        ExecutionFailedEvent failedEvent = new ExecutionFailedEvent(
                execution.getId(),
                jobId,
                orgId,
                "worker-local-1",
                1,
                Instant.now(),
                "Error processing report"
        );

        // First failure event
        executionService.handleExecutionFailed(failedEvent);

        // Duplicate failure event
        executionService.handleExecutionFailed(failedEvent);

        verify(kafkaExecutionRetryProducer, times(1)).sendExecutionRetry(any());
    }

    @Test
    void testDuplicateKafkaEventDoesNotCreateDuplicateExecution() {
        UUID eventId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Instant scheduledAt = Instant.now();

        JobTriggeredEvent event = new JobTriggeredEvent(eventId, jobId, orgId, scheduledAt, Instant.now(), "NORMAL");

        // First event processing
        Optional<Execution> first = executionService.createExecutionFromEvent(event);
        assertTrue(first.isPresent());

        // Duplicate event with same eventId
        Optional<Execution> second = executionService.createExecutionFromEvent(event);
        assertTrue(second.isPresent());

        assertEquals(1, executionRepository.count());
        verify(kafkaExecutionDispatchProducer, times(1)).sendExecutionDispatched(any(ExecutionDispatchedEvent.class));
    }
}
