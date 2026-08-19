package com.chronos.notification.provider;

import com.chronos.notification.entity.NotificationType;

import java.util.UUID;

/**
 * Immutable value object passed to a {@link NotificationProvider}.
 * Contains all information needed to deliver a notification without
 * the provider needing to access the database.
 */
public class NotificationRequest {

    private final UUID notificationId;
    private final UUID organizationId;
    private final UUID executionId;
    private final UUID jobId;
    private final NotificationType type;
    private final String recipient;
    private final String subject;
    private final String message;

    public NotificationRequest(UUID notificationId, UUID organizationId,
                               UUID executionId, UUID jobId,
                               NotificationType type, String recipient,
                               String subject, String message) {
        this.notificationId = notificationId;
        this.organizationId = organizationId;
        this.executionId = executionId;
        this.jobId = jobId;
        this.type = type;
        this.recipient = recipient;
        this.subject = subject;
        this.message = message;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "NotificationRequest{" +
                "notificationId=" + notificationId +
                ", organizationId=" + organizationId +
                ", executionId=" + executionId +
                ", jobId=" + jobId +
                ", type=" + type +
                ", recipient='" + recipient + '\'' +
                ", subject='" + subject + '\'' +
                '}';
    }
}
