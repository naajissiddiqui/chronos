package com.chronos.execution.event;

import java.time.Instant;
import java.util.UUID;

public class ExecutionFailedEvent {

    private UUID executionId;
    private UUID jobId;
    private UUID organizationId;
    private String workerId;
    private Integer attempt;
    private Instant failedAt;
    private String errorMessage;

    public ExecutionFailedEvent() {
    }

    public ExecutionFailedEvent(UUID executionId, UUID jobId, UUID organizationId, String workerId, Integer attempt, Instant failedAt, String errorMessage) {
        this.executionId = executionId;
        this.jobId = jobId;
        this.organizationId = organizationId;
        this.workerId = workerId;
        this.attempt = attempt;
        this.failedAt = failedAt;
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

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Integer getAttempt() {
        return attempt;
    }

    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
