package com.chronos.execution.service;

import com.chronos.execution.client.JobServiceClient;
import com.chronos.execution.dto.JobRetryConfigDto;
import com.chronos.execution.entity.Execution;
import com.chronos.execution.entity.ExecutionStatus;
import com.chronos.execution.event.ExecutionCompletedEvent;
import com.chronos.execution.event.ExecutionFailedEvent;
import com.chronos.execution.event.JobTriggeredEvent;
import com.chronos.execution.kafka.KafkaExecutionDispatchProducer;
import com.chronos.execution.kafka.KafkaExecutionDlqProducer;
import com.chronos.execution.kafka.KafkaExecutionRetryProducer;
import com.chronos.execution.repository.ExecutionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceMetricsTest {

    @Mock
    private ExecutionRepository executionRepository;
    @Mock
    private KafkaExecutionDispatchProducer dispatchProducer;
    @Mock
    private KafkaExecutionRetryProducer retryProducer;
    @Mock
    private KafkaExecutionDlqProducer dlqProducer;
    @Mock
    private JobServiceClient jobServiceClient;

    private MeterRegistry meterRegistry;
    private ExecutionService executionService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        executionService = new ExecutionService(
                executionRepository,
                dispatchProducer,
                retryProducer,
                dlqProducer,
                jobServiceClient,
                meterRegistry
        );
    }

    @Test
    void testCreateExecutionIncrementsCreatedCounter() {
        JobTriggeredEvent event = new JobTriggeredEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now(),
                "NORMAL"
        );

        when(executionRepository.findBySourceEventId(event.getEventId())).thenReturn(Optional.empty());
        Execution saved = new Execution(event.getJobId(), event.getOrganizationId(), event.getEventId(), ExecutionStatus.PENDING, 1, Instant.now());
        when(executionRepository.saveAndFlush(any(Execution.class))).thenReturn(saved);

        Optional<Execution> result = executionService.createExecutionFromEvent(event);

        assertEquals(1.0, meterRegistry.find("executions_created_total").counter().count());
    }

    @Test
    void testExecutionCompletedIncrementsSucceededCounterAndTimer() {
        UUID executionId = UUID.randomUUID();
        Execution execution = new Execution(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ExecutionStatus.PENDING, 1, Instant.now().minusSeconds(5));
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));

        ExecutionCompletedEvent event = new ExecutionCompletedEvent(executionId, execution.getJobId(), execution.getOrganizationId(), "worker-1", 1, Instant.now(), "Success");

        executionService.handleExecutionCompleted(event);

        assertEquals(1.0, meterRegistry.find("executions_succeeded_total").counter().count());
        assertEquals(1, meterRegistry.find("execution_duration").timer().count());
    }

    @Test
    void testExecutionFailedRetryScheduledIncrementsRetriedCounter() {
        UUID executionId = UUID.randomUUID();
        Execution execution = new Execution(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ExecutionStatus.PENDING, 1, Instant.now());
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        when(jobServiceClient.getJobRetryConfig(any(), any())).thenReturn(new JobRetryConfigDto(3, 10));

        ExecutionFailedEvent event = new ExecutionFailedEvent(executionId, execution.getJobId(), execution.getOrganizationId(), "worker-1", 1, Instant.now(), "Error");

        executionService.handleExecutionFailed(event);

        assertEquals(1.0, meterRegistry.find("executions_retried_total").counter().count());
    }

    @Test
    void testExecutionFailedRetriesExhaustedIncrementsFailedAndDlqCounters() {
        UUID executionId = UUID.randomUUID();
        Execution execution = new Execution(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ExecutionStatus.PENDING, 4, Instant.now().minusSeconds(10));
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        when(jobServiceClient.getJobRetryConfig(any(), any())).thenReturn(new JobRetryConfigDto(3, 10));

        ExecutionFailedEvent event = new ExecutionFailedEvent(executionId, execution.getJobId(), execution.getOrganizationId(), "worker-1", 4, Instant.now(), "Fatal Error");

        executionService.handleExecutionFailed(event);

        assertEquals(1.0, meterRegistry.find("executions_failed_total").counter().count());
        assertEquals(1.0, meterRegistry.find("executions_dead_lettered_total").counter().count());
        assertEquals(1, meterRegistry.find("execution_duration").timer().count());
    }
}
