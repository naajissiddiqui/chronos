package com.chronos.execution.service;

import com.chronos.execution.entity.Execution;
import com.chronos.execution.entity.ExecutionStatus;
import com.chronos.execution.event.ExecutionDispatchedEvent;
import com.chronos.execution.kafka.KafkaExecutionDispatchProducer;
import com.chronos.execution.repository.ExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RetrySchedulerServiceTest {

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private RetrySchedulerService retrySchedulerService;

    @MockBean
    private KafkaExecutionDispatchProducer kafkaExecutionDispatchProducer;

    @BeforeEach
    void setUp() {
        executionRepository.deleteAll();
        reset(kafkaExecutionDispatchProducer);
    }

    @Test
    void testPollAndDispatchDueRetriesDispatchesEventAndUpdatesStatusToPending() {
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID sourceEventId = UUID.randomUUID();

        Execution execution = new Execution(jobId, orgId, sourceEventId, ExecutionStatus.RETRY_SCHEDULED, 2, Instant.now().minusSeconds(60));
        execution.setNextAttemptAt(Instant.now().minusSeconds(5)); // Due retry
        executionRepository.saveAndFlush(execution);

        retrySchedulerService.pollAndDispatchDueRetries();

        Execution updated = executionRepository.findById(execution.getId()).orElseThrow();
        assertEquals(ExecutionStatus.PENDING, updated.getStatus());
        assertNull(updated.getNextAttemptAt());

        ArgumentCaptor<ExecutionDispatchedEvent> captor = ArgumentCaptor.forClass(ExecutionDispatchedEvent.class);
        verify(kafkaExecutionDispatchProducer, times(1)).sendExecutionDispatched(captor.capture());

        ExecutionDispatchedEvent dispatchEvent = captor.getValue();
        assertEquals(execution.getId(), dispatchEvent.getExecutionId());
        assertEquals(2, dispatchEvent.getAttempt());
        assertEquals(jobId, dispatchEvent.getJobId());
    }

    @Test
    void testPollAndDispatchSkipsRetriesNotYetDue() {
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID sourceEventId = UUID.randomUUID();

        Execution execution = new Execution(jobId, orgId, sourceEventId, ExecutionStatus.RETRY_SCHEDULED, 2, Instant.now());
        execution.setNextAttemptAt(Instant.now().plusSeconds(600)); // Future retry
        executionRepository.saveAndFlush(execution);

        retrySchedulerService.pollAndDispatchDueRetries();

        Execution updated = executionRepository.findById(execution.getId()).orElseThrow();
        assertEquals(ExecutionStatus.RETRY_SCHEDULED, updated.getStatus());
        verify(kafkaExecutionDispatchProducer, never()).sendExecutionDispatched(any());
    }
}
