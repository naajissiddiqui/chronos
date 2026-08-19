package com.chronos.notification.controller;

import com.chronos.notification.dto.NotificationResponse;
import com.chronos.notification.exception.UnauthorizedException;
import com.chronos.notification.service.NotificationService;
import com.chronos.notification.util.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped REST API for notifications.
 *
 * <p>All endpoints require the {@code X-Organization-Id} header (enforced by
 * {@link com.chronos.notification.config.TenantInterceptor}).
 * Organization A can never read Organization B's notifications.
 */
@RestController
@RequestMapping("/api/v1")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * GET /api/v1/notifications
     * Returns all notifications for the current organization.
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        UUID orgId = getRequiredOrganizationId();
        List<NotificationResponse> notifications = notificationService.getNotificationsForOrganization(orgId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * GET /api/v1/notifications/{id}
     * Returns a single notification by ID, scoped to the current organization.
     * Returns 404 if the notification belongs to a different organization.
     */
    @GetMapping("/notifications/{id}")
    public ResponseEntity<NotificationResponse> getNotificationById(@PathVariable("id") UUID id) {
        UUID orgId = getRequiredOrganizationId();
        NotificationResponse notification = notificationService.getNotificationByIdAndOrganization(id, orgId);
        return ResponseEntity.ok(notification);
    }

    /**
     * GET /api/v1/executions/{executionId}/notifications
     * Returns all notifications for a given execution, scoped to the current organization.
     */
    @GetMapping("/executions/{executionId}/notifications")
    public ResponseEntity<List<NotificationResponse>> getNotificationsForExecution(
            @PathVariable("executionId") UUID executionId) {
        UUID orgId = getRequiredOrganizationId();
        List<NotificationResponse> notifications =
                notificationService.getNotificationsForExecution(executionId, orgId);
        return ResponseEntity.ok(notifications);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private UUID getRequiredOrganizationId() {
        UUID orgId = TenantContext.getOrganizationId();
        if (orgId == null) {
            throw new UnauthorizedException("Missing required X-Organization-Id header");
        }
        return orgId;
    }
}
