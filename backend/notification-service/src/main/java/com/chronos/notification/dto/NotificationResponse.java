package com.chronos.notification.dto;

import com.chronos.notification.entity.Notification;
import com.chronos.notification.entity.NotificationStatus;
import com.chronos.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public class NotificationResponse {

    private UUID id;
    private UUID organizationId;
    private UUID executionId;
    private UUID jobId;
    private NotificationType type;
    private NotificationStatus status;
    private String recipient;
    private String subject;
    private String message;
    private Instant createdAt;
    private Instant sentAt;
    private String errorMessage;

    public NotificationResponse() {
    }

    public static NotificationResponse fromEntity(Notification n) {
        if (n == null) {
            return null;
        }
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setOrganizationId(n.getOrganizationId());
        r.setExecutionId(n.getExecutionId());
        r.setJobId(n.getJobId());
        r.setType(n.getType());
        r.setStatus(n.getStatus());
        r.setRecipient(n.getRecipient());
        r.setSubject(n.getSubject());
        r.setMessage(n.getMessage());
        r.setCreatedAt(n.getCreatedAt());
        r.setSentAt(n.getSentAt());
        r.setErrorMessage(n.getErrorMessage());
        return r;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public void setExecutionId(UUID executionId) {
        this.executionId = executionId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
