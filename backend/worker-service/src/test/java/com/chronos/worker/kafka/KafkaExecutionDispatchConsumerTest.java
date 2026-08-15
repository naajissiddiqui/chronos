package com.chronos.worker.kafka;

import com.chronos.worker.event.ExecutionCompletedEvent;
import com.chronos.worker.event.ExecutionDispatchedEvent;
import com.chronos.worker.event.ExecutionFailedEvent;
import com.chronos.worker.task.DemoReportTaskHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaExecutionDispatchConsumerTest {

    private DemoReportTaskHandler demoReportTaskHandler;
    private KafkaWorkerResultProducer resultProducer;
    private KafkaExecutionDispatchConsumer consumer;
    private final String workerId = "worker-test-1";

    @BeforeEach
    void setUp() {
        demoReportTaskHandler = mock(DemoReportTaskHandler.class);
        resultProducer = mock(KafkaWorkerResultProducer.class);
        consumer = new KafkaExecutionDispatchConsumer(workerId, demoReportTaskHandler, resultProducer);
    }

    @Test
    void testConsumeDemoReportTaskProducesCompletedEvent() {
        UUID executionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        ExecutionDispatchedEvent event = new ExecutionDispatchedEvent(
                executionId, jobId, orgId, 1, "DEMO_REPORT", "Payload", Instant.now()
        );

        when(demoReportTaskHandler.execute(event)).thenReturn("Demo report generated successfully for jobId=" + jobId);

        consumer.consume(event);

        verify(demoReportTaskHandler, times(1)).execute(event);

        ArgumentCaptor<ExecutionCompletedEvent> captor = ArgumentCaptor.forClass(ExecutionCompletedEvent.class);
        verify(resultProducer, times(1)).sendExecutionCompleted(captor.capture());

        ExecutionCompletedEvent completedEvent = captor.getValue();
        assertEquals(executionId, completedEvent.getExecutionId());
        assertEquals(jobId, completedEvent.getJobId());
        assertEquals(orgId, completedEvent.getOrganizationId());
        assertEquals(workerId, completedEvent.getWorkerId());
        assertEquals(1, completedEvent.getAttempt());
        assertEquals("Demo report generated successfully for jobId=" + jobId, completedEvent.getResult());
        assertNotNull(completedEvent.getCompletedAt());
    }

    @Test
    void testConsumeUnsupportedTaskTypeProducesFailedEvent() {
        UUID executionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        ExecutionDispatchedEvent event = new ExecutionDispatchedEvent(
                executionId, jobId, orgId, 1, "UNSUPPORTED_SHELL_COMMAND", "rm -rf /", Instant.now()
        );

        consumer.consume(event);

        verifyNoInteractions(demoReportTaskHandler);

        ArgumentCaptor<ExecutionFailedEvent> captor = ArgumentCaptor.forClass(ExecutionFailedEvent.class);
        verify(resultProducer, times(1)).sendExecutionFailed(captor.capture());

        ExecutionFailedEvent failedEvent = captor.getValue();
        assertEquals(executionId, failedEvent.getExecutionId());
        assertEquals(jobId, failedEvent.getJobId());
        assertEquals(orgId, failedEvent.getOrganizationId());
        assertEquals(workerId, failedEvent.getWorkerId());
        assertEquals(1, failedEvent.getAttempt());
        assertTrue(failedEvent.getErrorMessage().contains("Unsupported task type: UNSUPPORTED_SHELL_COMMAND"));
        assertNotNull(failedEvent.getFailedAt());
    }

    @Test
    void testDuplicateExecutionEventIsSkippedIdempotently() {
        UUID executionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        ExecutionDispatchedEvent event = new ExecutionDispatchedEvent(
                executionId, jobId, orgId, 1, "DEMO_REPORT", "Payload", Instant.now()
        );

        when(demoReportTaskHandler.execute(event)).thenReturn("Success");

        // First delivery
        consumer.consume(event);
        verify(demoReportTaskHandler, times(1)).execute(event);
        verify(resultProducer, times(1)).sendExecutionCompleted(any());

        // Duplicate delivery with same executionId and attempt
        consumer.consume(event);
        // Handler and producer should NOT be called a second time
        verify(demoReportTaskHandler, times(1)).execute(event);
        verify(resultProducer, times(1)).sendExecutionCompleted(any());
    }

    @Test
    void testRetryAttemptWithSameExecutionIdIsProcessed() {
        UUID executionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        ExecutionDispatchedEvent attempt1 = new ExecutionDispatchedEvent(
                executionId, jobId, orgId, 1, "DEMO_REPORT_FAIL", "Payload", Instant.now()
        );
        ExecutionDispatchedEvent attempt2 = new ExecutionDispatchedEvent(
                executionId, jobId, orgId, 2, "DEMO_REPORT_FAIL", "Payload", Instant.now()
        );

        when(demoReportTaskHandler.execute(any())).thenThrow(new RuntimeException("Controlled failure"));

        // Attempt 1 delivery
        consumer.consume(attempt1);
        verify(resultProducer, times(1)).sendExecutionFailed(any());

        // Attempt 2 delivery (retry with same executionId, attempt=2)
        consumer.consume(attempt2);
        // Should process attempt 2 and send second failure event
        verify(resultProducer, times(2)).sendExecutionFailed(any());
    }
}
