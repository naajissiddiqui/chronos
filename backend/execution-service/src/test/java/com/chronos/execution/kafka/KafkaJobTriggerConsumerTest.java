package com.chronos.execution.kafka;

import com.chronos.execution.event.JobTriggeredEvent;
import com.chronos.execution.service.ExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class KafkaJobTriggerConsumerTest {

    private ExecutionService executionService;
    private KafkaJobTriggerConsumer consumer;

    @BeforeEach
    void setUp() {
        executionService = mock(ExecutionService.class);
        consumer = new KafkaJobTriggerConsumer(executionService);
    }

    @Test
    void testConsumeEventInvokesService() {
        JobTriggeredEvent event = new JobTriggeredEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now(),
                "NORMAL"
        );

        consumer.consume(event);

        verify(executionService, times(1)).createExecutionFromEvent(event);
    }

    @Test
    void testConsumeNullEventHandlesGracefully() {
        assertDoesNotThrow(() -> consumer.consume(null));
        verifyNoInteractions(executionService);
    }

    @Test
    void testConsumeServiceFailureRethrowsExceptionForKafkaRedelivery() {
        JobTriggeredEvent event = new JobTriggeredEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now(),
                "HIGH"
        );

        doThrow(new RuntimeException("Database error")).when(executionService).createExecutionFromEvent(event);

        assertThrows(RuntimeException.class, () -> consumer.consume(event));
    }
}
