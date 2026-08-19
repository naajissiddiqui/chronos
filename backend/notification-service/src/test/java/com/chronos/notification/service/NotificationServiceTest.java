package com.chronos.notification.service;

import com.chronos.notification.entity.Notification;
import com.chronos.notification.entity.NotificationStatus;
import com.chronos.notification.entity.NotificationType;
import com.chronos.notification.event.ExecutionCompletedEvent;
import com.chronos.notification.event.ExecutionFailedEvent;
import com.chronos.notification.exception.ResourceNotFoundException;
import com.chronos.notification.provider.NotificationProvider;
import com.chronos.notification.provider.NotificationResult;
import com.chronos.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private NotificationProvider notificationProvider;

    @BeforeAll
    static void initUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        // Default: provider succeeds
        when(notificationProvider.send(any())).thenReturn(NotificationResult.success());
    }

    // -------------------------------------------------------------------------
    // execution.completed → notification created
    // -------------------------------------------------------------------------

    @Test
    void testProcessExecutionCompleted_CreatesNotificationWithStatusSent() {
        UUID executionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        ExecutionCompletedEvent event = new ExecutionCompletedEvent(
                executionId, jobId, orgId, "worker-1", 1, Instant.now(), "OK");

        notificationService.processExecutionCompleted(event);

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());

        Notification n = notifications.get(0);
        assertEquals(executionId, n.getExecutionId());
        assertEquals(jobId, n.getJobId());
        assertEquals(orgId, n.getOrganizationId());
        assertEquals(NotificationType.EXECUTION_SUCCEEDED, n.getType());
        assertEquals(NotificationStatus.SENT, n.getStatus());
        assertNotNull(n.getSentAt());
        assertNull(n.getErrorMessage());
        assertNotNull(n.getId());
        assertNotNull(n.getCreatedAt());
    }

    // -------------------------------------------------------------------------
    // execution.failed → notification created
    // -------------------------------------------------------------------------

    @Test
    void testProcessExecutionFailed_CreatesNotificationWithStatusSent() {
        UUID executionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        ExecutionFailedEvent event = new ExecutionFailedEvent(
                executionId, jobId, orgId, "worker-2", 3, Instant.now(), "Task timed out");

        notificationService.processExecutionFailed(event);

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());

        Notification n = notifications.get(0);
        assertEquals(executionId, n.getExecutionId());
        assertEquals(jobId, n.getJobId());
        assertEquals(orgId, n.getOrganizationId());
        assertEquals(NotificationType.EXECUTION_FAILED, n.getType());
        assertEquals(NotificationStatus.SENT, n.getStatus());
        assertNotNull(n.getSentAt());
        assertTrue(n.getMessage().contains("Task timed out"));
    }

    // -------------------------------------------------------------------------
    // Idempotency: duplicate events must not create duplicate notifications
    // -------------------------------------------------------------------------

    @Test
    void testDuplicateCompletedEvent_DoesNotCreateDuplicateNotification() {
        UUID executionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        ExecutionCompletedEvent event = new ExecutionCompletedEvent(
                executionId, jobId, orgId, "worker-1", 1, Instant.now(), "OK");

        // First delivery
        notificationService.processExecutionCompleted(event);
        // Duplicate delivery
        notificationService.processExecutionCompleted(event);

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size(), "Duplicate event must not create a second notification");

        // Provider called exactly once
        verify(notificationProvider, times(1)).send(any());
    }

    @Test
    void testDuplicateFailedEvent_DoesNotCreateDuplicateNotification() {
        UUID executionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        ExecutionFailedEvent event = new ExecutionFailedEvent(
                executionId, jobId, orgId, "worker-2", 3, Instant.now(), "OOM");

        // First delivery
        notificationService.processExecutionFailed(event);
        // Duplicate delivery
        notificationService.processExecutionFailed(event);

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size(), "Duplicate event must not create a second notification");

        verify(notificationProvider, times(1)).send(any());
    }

    // -------------------------------------------------------------------------
    // Provider success → SENT
    // -------------------------------------------------------------------------

    @Test
    void testProviderSuccess_SetsStatusSentWithSentAt() {
        when(notificationProvider.send(any())).thenReturn(NotificationResult.success());

        UUID executionId = UUID.randomUUID();
        ExecutionCompletedEvent event = new ExecutionCompletedEvent(
                executionId, UUID.randomUUID(), UUID.randomUUID(), "worker-1", 1, Instant.now(), "OK");

        notificationService.processExecutionCompleted(event);

        Notification n = notificationRepository.findAll().get(0);
        assertEquals(NotificationStatus.SENT, n.getStatus());
        assertNotNull(n.getSentAt());
        assertNull(n.getErrorMessage());
    }

    // -------------------------------------------------------------------------
    // Provider failure → FAILED
    // -------------------------------------------------------------------------

    @Test
    void testProviderFailure_SetsStatusFailedWithErrorMessage() {
        when(notificationProvider.send(any()))
                .thenReturn(NotificationResult.failure("SMTP connection refused"));

        UUID executionId = UUID.randomUUID();
        ExecutionCompletedEvent event = new ExecutionCompletedEvent(
                executionId, UUID.randomUUID(), UUID.randomUUID(), "worker-1", 1, Instant.now(), "OK");

        notificationService.processExecutionCompleted(event);

        Notification n = notificationRepository.findAll().get(0);
        assertEquals(NotificationStatus.FAILED, n.getStatus());
        assertNull(n.getSentAt());
        assertEquals("SMTP connection refused", n.getErrorMessage());
    }

    // -------------------------------------------------------------------------
    // Tenant isolation
    // -------------------------------------------------------------------------

    @Test
    void testTenantIsolation_OrgACannotReadOrgBNotifications() {
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();

        ExecutionCompletedEvent eventA = new ExecutionCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), orgA, "w1", 1, Instant.now(), "OK");
        ExecutionFailedEvent eventB = new ExecutionFailedEvent(
                UUID.randomUUID(), UUID.randomUUID(), orgB, "w2", 1, Instant.now(), "err");

        notificationService.processExecutionCompleted(eventA);
        notificationService.processExecutionFailed(eventB);

        // Org A sees only its own notification
        List<com.chronos.notification.dto.NotificationResponse> orgANotifs =
                notificationService.getNotificationsForOrganization(orgA);
        assertEquals(1, orgANotifs.size());
        assertEquals(orgA, orgANotifs.get(0).getOrganizationId());

        // Org B sees only its own notification
        List<com.chronos.notification.dto.NotificationResponse> orgBNotifs =
                notificationService.getNotificationsForOrganization(orgB);
        assertEquals(1, orgBNotifs.size());
        assertEquals(orgB, orgBNotifs.get(0).getOrganizationId());
    }

    // -------------------------------------------------------------------------
    // Notification lookup
    // -------------------------------------------------------------------------

    @Test
    void testGetNotificationById_ReturnsCorrectNotification() {
        UUID orgId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();

        ExecutionCompletedEvent event = new ExecutionCompletedEvent(
                executionId, UUID.randomUUID(), orgId, "w1", 1, Instant.now(), "OK");
        notificationService.processExecutionCompleted(event);

        Notification saved = notificationRepository.findAll().get(0);
        com.chronos.notification.dto.NotificationResponse response =
                notificationService.getNotificationByIdAndOrganization(saved.getId(), orgId);

        assertNotNull(response);
        assertEquals(saved.getId(), response.getId());
        assertEquals(orgId, response.getOrganizationId());
    }

    @Test
    void testGetNotificationById_WrongOrg_ThrowsResourceNotFoundException() {
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();

        ExecutionCompletedEvent event = new ExecutionCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), orgA, "w1", 1, Instant.now(), "OK");
        notificationService.processExecutionCompleted(event);

        Notification saved = notificationRepository.findAll().get(0);

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.getNotificationByIdAndOrganization(saved.getId(), orgB));
    }

    // -------------------------------------------------------------------------
    // Execution notification lookup
    // -------------------------------------------------------------------------

    @Test
    void testGetNotificationsForExecution_ReturnsCorrectResults() {
        UUID orgId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();

        ExecutionCompletedEvent completedEvent = new ExecutionCompletedEvent(
                executionId, UUID.randomUUID(), orgId, "w1", 1, Instant.now(), "OK");

        // Unrelated notification for a different execution
        ExecutionCompletedEvent otherEvent = new ExecutionCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), orgId, "w2", 1, Instant.now(), "OK");

        notificationService.processExecutionCompleted(completedEvent);
        notificationService.processExecutionCompleted(otherEvent);

        List<com.chronos.notification.dto.NotificationResponse> results =
                notificationService.getNotificationsForExecution(executionId, orgId);

        assertEquals(1, results.size());
        assertEquals(executionId, results.get(0).getExecutionId());
    }

    // -------------------------------------------------------------------------
    // Invalid event handling
    // -------------------------------------------------------------------------

    @Test
    void testNullCompletedEvent_HandledGracefully() {
        assertDoesNotThrow(() -> notificationService.processExecutionCompleted(null));
        assertEquals(0, notificationRepository.count());
        verifyNoInteractions(notificationProvider);
    }

    @Test
    void testNullFailedEvent_HandledGracefully() {
        assertDoesNotThrow(() -> notificationService.processExecutionFailed(null));
        assertEquals(0, notificationRepository.count());
        verifyNoInteractions(notificationProvider);
    }

    @Test
    void testCompletedEventWithNullExecutionId_HandledGracefully() {
        ExecutionCompletedEvent event = new ExecutionCompletedEvent(
                null, UUID.randomUUID(), UUID.randomUUID(), "w1", 1, Instant.now(), "OK");
        assertDoesNotThrow(() -> notificationService.processExecutionCompleted(event));
        assertEquals(0, notificationRepository.count());
    }

    @Test
    void testFailedEventWithNullOrganizationId_HandledGracefully() {
        ExecutionFailedEvent event = new ExecutionFailedEvent(
                UUID.randomUUID(), UUID.randomUUID(), null, "w1", 1, Instant.now(), "err");
        assertDoesNotThrow(() -> notificationService.processExecutionFailed(event));
        assertEquals(0, notificationRepository.count());
    }
}
