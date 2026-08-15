package com.chronos.execution.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ExecutionRetryEvent {

    private UUID executionId;
    private UUID jobId;
    private UUID organizationId;
    private Integer attempt;
    private Instant nextAttemptAt;
    private String reason;
    private Instant scheduledAt;
    private String taskType;
    private String payload;

    public ExecutionRetryEvent() {
    }

    public ExecutionRetryEvent(UUID executionId, UUID jobId, UUID organizationId, Integer attempt, Instant nextAttemptAt, String reason, Instant scheduledAt, String taskType, String payload) {
        this.executionId = executionId;
        this.jobId = jobId;
        this.organizationId = organizationId;
        this.attempt = attempt;
        this.nextAttemptAt = nextAttemptAt;
        this.reason = reason;
        this.scheduledAt = scheduledAt;
        this.taskType = taskType;
        this.payload = payload;
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

    public Integer getAttempt() {
        return attempt;
    }

    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionRetryEvent that = (ExecutionRetryEvent) o;
        return Objects.equals(executionId, that.executionId) &&
                Objects.equals(jobId, that.jobId) &&
                Objects.equals(organizationId, that.organizationId) &&
                Objects.equals(attempt, that.attempt) &&
                Objects.equals(nextAttemptAt, that.nextAttemptAt) &&
                Objects.equals(reason, that.reason) &&
                Objects.equals(scheduledAt, that.scheduledAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionId, jobId, organizationId, attempt, nextAttemptAt, reason, scheduledAt);
    }

    @Override
    public String toString() {
        return "ExecutionRetryEvent{" +
                "executionId=" + executionId +
                ", jobId=" + jobId +
                ", organizationId=" + organizationId +
                ", attempt=" + attempt +
                ", nextAttemptAt=" + nextAttemptAt +
                ", reason='" + reason + '\'' +
                ", scheduledAt=" + scheduledAt +
                '}';
    }
}
