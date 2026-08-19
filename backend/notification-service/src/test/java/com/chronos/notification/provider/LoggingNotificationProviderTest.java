package com.chronos.notification.provider;

import com.chronos.notification.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the LoggingNotificationProvider.
 * Verifies that the provider returns a success result and does not throw.
 */
class LoggingNotificationProviderTest {

    private LoggingNotificationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LoggingNotificationProvider();
    }

    @Test
    void testSend_ReturnsSuccessResult() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                NotificationType.EXECUTION_SUCCEEDED,
                "org-123@notifications.chronos.internal",
                "Execution succeeded: abc-123",
                "Job execution completed successfully."
        );

        NotificationResult result = provider.send(request);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNull(result.getErrorMessage());
    }

    @Test
    void testSend_ForFailedType_ReturnsSuccessResult() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                NotificationType.EXECUTION_FAILED,
                "org-456@notifications.chronos.internal",
                "Execution failed: xyz-789",
                "Job execution failed after 3 attempts."
        );

        NotificationResult result = provider.send(request);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNull(result.getErrorMessage());
    }

    @Test
    void testSend_DoesNotThrow() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                NotificationType.EXECUTION_SUCCEEDED,
                "recipient@example.com",
                "Test subject",
                "Test message body"
        );

        assertDoesNotThrow(() -> provider.send(request));
    }

    @Test
    void testNotificationResult_SuccessFactory() {
        NotificationResult result = NotificationResult.success();
        assertTrue(result.isSuccess());
        assertNull(result.getErrorMessage());
    }

    @Test
    void testNotificationResult_FailureFactory() {
        NotificationResult result = NotificationResult.failure("Connection refused");
        assertFalse(result.isSuccess());
        assertEquals("Connection refused", result.getErrorMessage());
    }
}
