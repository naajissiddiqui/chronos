package com.chronos.execution.service;

import com.chronos.execution.dto.ExecutionResponse;
import com.chronos.execution.entity.Execution;
import com.chronos.execution.entity.ExecutionStatus;
import com.chronos.execution.event.JobTriggeredEvent;
import com.chronos.execution.repository.ExecutionRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExecutionServiceTest {

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private ExecutionService executionService;

    @BeforeAll
    static void initUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setUp() {
        executionRepository.deleteAll();
    }

    @Test
    void testJobTriggeredEventCreatesPendingExecutionWithAttemptOne() {
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
        assertEquals(scheduledAt, execution.getScheduledAt());
        assertNotNull(execution.getCreatedAt());
        assertNotNull(execution.getUpdatedAt());
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
        assertEquals(first.get().getId(), second.get().getId(), "Duplicate event must return existing execution without creating new record");

        // Total count in DB must remain 1
        assertEquals(1, executionRepository.count());
    }

    @Test
    void testGetExecutionsScopedToOrganization() {
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();
        UUID jobIdA = UUID.randomUUID();
        UUID jobIdB = UUID.randomUUID();

        JobTriggeredEvent eventA = new JobTriggeredEvent(UUID.randomUUID(), jobIdA, orgA, Instant.now(), Instant.now(), "NORMAL");
        JobTriggeredEvent eventB = new JobTriggeredEvent(UUID.randomUUID(), jobIdB, orgB, Instant.now(), Instant.now(), "NORMAL");

        executionService.createExecutionFromEvent(eventA);
        executionService.createExecutionFromEvent(eventB);

        List<ExecutionResponse> orgAExecutions = executionService.getExecutionsForOrganization(orgA);
        assertEquals(1, orgAExecutions.size());
        assertEquals(jobIdA, orgAExecutions.get(0).getJobId());

        List<ExecutionResponse> orgBExecutions = executionService.getExecutionsForOrganization(orgB);
        assertEquals(1, orgBExecutions.size());
        assertEquals(jobIdB, orgBExecutions.get(0).getJobId());
    }
}
