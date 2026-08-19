package com.chronos.notification.controller;

import com.chronos.notification.entity.Notification;
import com.chronos.notification.entity.NotificationStatus;
import com.chronos.notification.entity.NotificationType;
import com.chronos.notification.event.ExecutionCompletedEvent;
import com.chronos.notification.event.ExecutionFailedEvent;
import com.chronos.notification.provider.NotificationProvider;
import com.chronos.notification.provider.NotificationResult;
import com.chronos.notification.repository.NotificationRepository;
import com.chronos.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private NotificationProvider notificationProvider;

    private UUID orgA;
    private UUID orgB;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        orgA = UUID.randomUUID();
        orgB = UUID.randomUUID();
        // Default: provider succeeds
        when(notificationProvider.send(any())).thenReturn(NotificationResult.success());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/notifications — tenant isolation
    // -------------------------------------------------------------------------

    @Test
    void testGetAllNotifications_ReturnOnlyOwnOrganizationNotifications() throws Exception {
        // Create one notification for orgA and one for orgB
        ExecutionCompletedEvent eventA = new ExecutionCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), orgA, "w1", 1, Instant.now(), "OK");
        ExecutionFailedEvent eventB = new ExecutionFailedEvent(
                UUID.randomUUID(), UUID.randomUUID(), orgB, "w2", 1, Instant.now(), "err");

        notificationService.processExecutionCompleted(eventA);
        notificationService.processExecutionFailed(eventB);

        // Org A sees only its notification
        mockMvc.perform(get("/api/v1/notifications")
                        .header("X-Organization-Id", orgA.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].organizationId").value(orgA.toString()))
                .andExpect(jsonPath("$[0].type").value("EXECUTION_SUCCEEDED"))
                .andExpect(jsonPath("$[0].status").value("SENT"));

        // Org B sees only its notification
        mockMvc.perform(get("/api/v1/notifications")
                        .header("X-Organization-Id", orgB.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].organizationId").value(orgB.toString()))
                .andExpect(jsonPath("$[0].type").value("EXECUTION_FAILED"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/notifications/{id} — org-scoped, 404 for wrong org
    // -------------------------------------------------------------------------

    @Test
    void testGetNotificationById_CorrectOrg_Returns200() throws Exception {
        ExecutionCompletedEvent event = new ExecutionCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), orgA, "w1", 1, Instant.now(), "OK");
        notificationService.processExecutionCompleted(event);

        Notification saved = notificationRepository.findAll().get(0);

        mockMvc.perform(get("/api/v1/notifications/" + saved.getId())
                        .header("X-Organization-Id", orgA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.organizationId").value(orgA.toString()))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void testGetNotificationById_WrongOrg_Returns404() throws Exception {
        ExecutionCompletedEvent event = new ExecutionCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), orgA, "w1", 1, Instant.now(), "OK");
        notificationService.processExecutionCompleted(event);

        Notification saved = notificationRepository.findAll().get(0);

        // Org B cannot see Org A's notification
        mockMvc.perform(get("/api/v1/notifications/" + saved.getId())
                        .header("X-Organization-Id", orgB.toString()))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/executions/{executionId}/notifications
    // -------------------------------------------------------------------------

    @Test
    void testGetNotificationsForExecution_ReturnsOnlyMatchingNotifications() throws Exception {
        UUID targetExecutionId = UUID.randomUUID();

        ExecutionCompletedEvent targetEvent = new ExecutionCompletedEvent(
                targetExecutionId, UUID.randomUUID(), orgA, "w1", 1, Instant.now(), "OK");
        ExecutionCompletedEvent otherEvent = new ExecutionCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), orgA, "w2", 1, Instant.now(), "OK");

        notificationService.processExecutionCompleted(targetEvent);
        notificationService.processExecutionCompleted(otherEvent);

        mockMvc.perform(get("/api/v1/executions/" + targetExecutionId + "/notifications")
                        .header("X-Organization-Id", orgA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].executionId").value(targetExecutionId.toString()));
    }

    @Test
    void testGetNotificationsForExecution_WrongOrg_ReturnsEmpty() throws Exception {
        UUID executionId = UUID.randomUUID();

        ExecutionCompletedEvent event = new ExecutionCompletedEvent(
                executionId, UUID.randomUUID(), orgA, "w1", 1, Instant.now(), "OK");
        notificationService.processExecutionCompleted(event);

        // Org B queries Org A's execution — returns empty, not error
        mockMvc.perform(get("/api/v1/executions/" + executionId + "/notifications")
                        .header("X-Organization-Id", orgB.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // -------------------------------------------------------------------------
    // Missing X-Organization-Id → 401
    // -------------------------------------------------------------------------

    @Test
    void testMissingOrganizationHeader_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testMissingOrganizationHeader_OnNotificationById_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testMissingOrganizationHeader_OnExecutionNotifications_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/executions/" + UUID.randomUUID() + "/notifications"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Cross-org isolation verification
    // -------------------------------------------------------------------------

    @Test
    void testCrossOrgIsolation_OrgACannotAccessOrgBNotifications() throws Exception {
        // Create notifications for both orgs
        ExecutionCompletedEvent eventA = new ExecutionCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), orgA, "w1", 1, Instant.now(), "OK");
        ExecutionCompletedEvent eventB = new ExecutionCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), orgB, "w2", 1, Instant.now(), "OK");

        notificationService.processExecutionCompleted(eventA);
        notificationService.processExecutionCompleted(eventB);

        // Verify total in DB = 2
        org.junit.jupiter.api.Assertions.assertEquals(2, notificationRepository.count());

        // Org A request returns exactly 1 (its own)
        mockMvc.perform(get("/api/v1/notifications")
                        .header("X-Organization-Id", orgA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].organizationId").value(orgA.toString()));

        // Org B request returns exactly 1 (its own)
        mockMvc.perform(get("/api/v1/notifications")
                        .header("X-Organization-Id", orgB.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].organizationId").value(orgB.toString()));
    }

    // -------------------------------------------------------------------------
    // Response shape verification
    // -------------------------------------------------------------------------

    @Test
    void testNotificationResponseShape_ContainsAllRequiredFields() throws Exception {
        UUID executionId = UUID.randomUUID();
        ExecutionCompletedEvent event = new ExecutionCompletedEvent(
                executionId, UUID.randomUUID(), orgA, "w1", 1, Instant.now(), "OK");
        notificationService.processExecutionCompleted(event);

        mockMvc.perform(get("/api/v1/notifications")
                        .header("X-Organization-Id", orgA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].organizationId").exists())
                .andExpect(jsonPath("$[0].executionId").value(executionId.toString()))
                .andExpect(jsonPath("$[0].jobId").exists())
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[0].status").exists())
                .andExpect(jsonPath("$[0].recipient").exists())
                .andExpect(jsonPath("$[0].subject").exists())
                .andExpect(jsonPath("$[0].message").exists())
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].sentAt").exists());
    }
}
