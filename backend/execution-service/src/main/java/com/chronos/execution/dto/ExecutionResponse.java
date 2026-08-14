package com.chronos.execution.dto;

import com.chronos.execution.entity.Execution;
import com.chronos.execution.entity.ExecutionStatus;

import java.time.Instant;
import java.util.UUID;

public class ExecutionResponse {

    private UUID id;
    private UUID jobId;
    private UUID organizationId;
    private UUID sourceEventId;
    private ExecutionStatus status;
    private Integer attempt;
    private Instant scheduledAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private String errorMessage;
    private String workerId;
    private String result;

    public ExecutionResponse() {
    }

    public static ExecutionResponse fromEntity(Execution execution) {
        if (execution == null) {
            return null;
        }
        ExecutionResponse response = new ExecutionResponse();
        response.setId(execution.getId());
        response.setJobId(execution.getJobId());
        response.setOrganizationId(execution.getOrganizationId());
        response.setSourceEventId(execution.getSourceEventId());
        response.setStatus(execution.getStatus());
        response.setAttempt(execution.getAttempt());
        response.setScheduledAt(execution.getScheduledAt());
        response.setStartedAt(execution.getStartedAt());
        response.setCompletedAt(execution.getCompletedAt());
        response.setCreatedAt(execution.getCreatedAt());
        response.setUpdatedAt(execution.getUpdatedAt());
        response.setErrorMessage(execution.getErrorMessage());
        response.setWorkerId(execution.getWorkerId());
        response.setResult(execution.getResult());
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(UUID sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public Integer getAttempt() {
        return attempt;
    }

    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
