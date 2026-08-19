package com.chronos.notification.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event consumed from the {@code execution.completed} topic.
 *
 * <p>This is an independent POJO copy of the event schema produced by the
 * Execution Service. Keeping it local avoids compile-time coupling between
 * microservices while remaining wire-compatible with the same JSON schema.
 */
public class ExecutionCompletedEvent {

    private UUID executionId;
    private UUID jobId;
    private UUID organizationId;
    private String workerId;
    private Integer attempt;
    private Instant completedAt;
    private String result;

    public ExecutionCompletedEvent() {
    }

    public ExecutionCompletedEvent(UUID executionId, UUID jobId, UUID organizationId,
                                    String workerId, Integer attempt,
                                    Instant completedAt, String result) {
        this.executionId = executionId;
        this.jobId = jobId;
        this.organizationId = organizationId;
        this.workerId = workerId;
        this.attempt = attempt;
        this.completedAt = completedAt;
        this.result = result;
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

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
