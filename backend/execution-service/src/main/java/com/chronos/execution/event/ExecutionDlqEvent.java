package com.chronos.execution.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ExecutionDlqEvent {

    private UUID executionId;
    private UUID jobId;
    private UUID organizationId;
    private Integer finalAttempt;
    private String reason;
    private Instant failedAt;
    private String lastWorkerId;
    private String errorMessage;

    public ExecutionDlqEvent() {
    }

    public ExecutionDlqEvent(UUID executionId, UUID jobId, UUID organizationId, Integer finalAttempt, String reason, Instant failedAt, String lastWorkerId, String errorMessage) {
        this.executionId = executionId;
        this.jobId = jobId;
        this.organizationId = organizationId;
        this.finalAttempt = finalAttempt;
        this.reason = reason;
        this.failedAt = failedAt;
        this.lastWorkerId = lastWorkerId;
        this.errorMessage = errorMessage;
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

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public Integer getFinalAttempt() {
        return finalAttempt;
    }

    public void setFinalAttempt(Integer finalAttempt) {
        this.finalAttempt = finalAttempt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public String getLastWorkerId() {
        return lastWorkerId;
    }

    public void setLastWorkerId(String lastWorkerId) {
        this.lastWorkerId = lastWorkerId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionDlqEvent that = (ExecutionDlqEvent) o;
        return Objects.equals(executionId, that.executionId) &&
                Objects.equals(jobId, that.jobId) &&
                Objects.equals(organizationId, that.organizationId) &&
                Objects.equals(finalAttempt, that.finalAttempt) &&
                Objects.equals(failedAt, that.failedAt) &&
                Objects.equals(lastWorkerId, that.lastWorkerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionId, jobId, organizationId, finalAttempt, failedAt, lastWorkerId);
    }

    @Override
    public String toString() {
        return "ExecutionDlqEvent{" +
                "executionId=" + executionId +
                ", jobId=" + jobId +
                ", organizationId=" + organizationId +
                ", finalAttempt=" + finalAttempt +
                ", reason='" + reason + '\'' +
                ", failedAt=" + failedAt +
                ", lastWorkerId='" + lastWorkerId + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
