package com.chronos.notification.kafka;

import com.chronos.notification.event.ExecutionCompletedEvent;
import com.chronos.notification.event.ExecutionFailedEvent;
import com.chronos.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Kafka consumer. Uses plain Mockito (no Spring context)
 * to verify consumer behaviour independently of the broker.
 */
class KafkaNotificationConsumerTest {

    private NotificationService notificationService;
    private KafkaNotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        consumer = new KafkaNotificationConsumer(notificationService);
    }

    // -------------------------------------------------------------------------
    // execution.completed consumer
    // -------------------------------------------------------------------------

    @Test
    void testConsumeCompleted_ValidEvent_InvokesService() {
        ExecutionCompletedEvent event = new ExecutionCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "worker-1", 1, Instant.now(), "OK");

        consumer.consumeCompleted(event);

        verify(notificationService, times(1)).processExecutionCompleted(event);
    }

    @Test
    void testConsumeCompleted_NullEvent_HandlesGracefully_NoServiceCall() {
        assertDoesNotThrow(() -> consumer.consumeCompleted(null));
        verifyNoInteractions(notificationService);
    }

    @Test
    void testConsumeCompleted_NullExecutionId_HandlesGracefully_NoServiceCall() {
        ExecutionCompletedEvent event = new ExecutionCompletedEvent(
                null, UUID.randomUUID(), UUID.randomUUID(), "w1", 1, Instant.now(), "OK");

        assertDoesNotThrow(() -> consumer.consumeCompleted(event));
        verifyNoInteractions(notificationService);
    }

    @Test
    void testConsumeCompleted_ServiceThrows_ExceptionRethrown_ForKafkaRedelivery() {
        ExecutionCompletedEvent event = new ExecutionCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "worker-1", 1, Instant.now(), "OK");

        doThrow(new RuntimeException("DB unavailable"))
                .when(notificationService).processExecutionCompleted(event);

        // Exception must propagate so Kafka does NOT commit the offset
        assertThrows(RuntimeException.class, () -> consumer.consumeCompleted(event));
    }

    // -------------------------------------------------------------------------
    // execution.failed consumer
    // -------------------------------------------------------------------------

    @Test
    void testConsumeFailed_ValidEvent_InvokesService() {
        ExecutionFailedEvent event = new ExecutionFailedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "worker-2", 3, Instant.now(), "OOM error");

        consumer.consumeFailed(event);

        verify(notificationService, times(1)).processExecutionFailed(event);
    }

    @Test
    void testConsumeFailed_NullEvent_HandlesGracefully_NoServiceCall() {
        assertDoesNotThrow(() -> consumer.consumeFailed(null));
        verifyNoInteractions(notificationService);
    }

    @Test
    void testConsumeFailed_NullExecutionId_HandlesGracefully_NoServiceCall() {
        ExecutionFailedEvent event = new ExecutionFailedEvent(
                null, UUID.randomUUID(), UUID.randomUUID(), "w1", 1, Instant.now(), "err");

        assertDoesNotThrow(() -> consumer.consumeFailed(event));
        verifyNoInteractions(notificationService);
    }

    @Test
    void testConsumeFailed_ServiceThrows_ExceptionRethrown_ForKafkaRedelivery() {
        ExecutionFailedEvent event = new ExecutionFailedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "worker-2", 3, Instant.now(), "timeout");

        doThrow(new RuntimeException("Transaction failed"))
                .when(notificationService).processExecutionFailed(event);

        assertThrows(RuntimeException.class, () -> consumer.consumeFailed(event));
    }
}
