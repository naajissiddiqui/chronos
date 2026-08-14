package com.chronos.execution.service;

import com.chronos.execution.dto.ExecutionResponse;
import com.chronos.execution.entity.Execution;
import com.chronos.execution.entity.ExecutionStatus;
import com.chronos.execution.event.ExecutionCompletedEvent;
import com.chronos.execution.event.ExecutionDispatchedEvent;
import com.chronos.execution.event.ExecutionFailedEvent;
import com.chronos.execution.event.JobTriggeredEvent;
import com.chronos.execution.kafka.KafkaExecutionDispatchProducer;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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

    @BeforeAll
    static void initUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setUp() {
        executionRepository.deleteAll();
        reset(kafkaExecutionDispatchProducer);
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
    }

    @Test
    void testHandleExecutionFailedUpdatesStatusToFailed() {
        UUID eventId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        JobTriggeredEvent event = new JobTriggeredEvent(eventId, jobId, orgId, Instant.now(), Instant.now(), "HIGH");
        Execution execution = executionService.createExecutionFromEvent(event).orElseThrow();

        ExecutionFailedEvent failedEvent = new ExecutionFailedEvent(
                execution.getId(),
                jobId,
                orgId,
                "worker-local-1",
                1,
                Instant.now(),
                "Unsupported task type: INVALID"
        );

        executionService.handleExecutionFailed(failedEvent);

        Execution updated = executionRepository.findById(execution.getId()).orElseThrow();
        assertEquals(ExecutionStatus.FAILED, updated.getStatus());
        assertEquals("worker-local-1", updated.getWorkerId());
        assertEquals("Unsupported task type: INVALID", updated.getErrorMessage());
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
