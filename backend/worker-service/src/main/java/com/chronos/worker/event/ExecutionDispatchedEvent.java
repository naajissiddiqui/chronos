package com.chronos.worker.event;

import java.time.Instant;
import java.util.UUID;

public class ExecutionDispatchedEvent {

    private UUID executionId;
    private UUID jobId;
    private UUID organizationId;
    private Integer attempt;
    private String taskType;
    private String payload;
    private Instant dispatchedAt;

    public ExecutionDispatchedEvent() {
    }

    public ExecutionDispatchedEvent(UUID executionId, UUID jobId, UUID organizationId, Integer attempt, String taskType, String payload, Instant dispatchedAt) {
        this.executionId = executionId;
        this.jobId = jobId;
        this.organizationId = organizationId;
        this.attempt = attempt;
        this.taskType = taskType;
        this.payload = payload;
        this.dispatchedAt = dispatchedAt;
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

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(Instant dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }
}
