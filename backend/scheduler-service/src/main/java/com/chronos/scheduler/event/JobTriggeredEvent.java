package com.chronos.scheduler.event;

import com.chronos.scheduler.entity.JobPriority;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class JobTriggeredEvent {

    private UUID eventId;
    private UUID jobId;
    private UUID organizationId;
    private Instant scheduledAt;
    private Instant triggeredAt;
    private JobPriority priority;

    public JobTriggeredEvent() {
    }

    public JobTriggeredEvent(UUID eventId, UUID jobId, UUID organizationId, Instant scheduledAt, Instant triggeredAt, JobPriority priority) {
        this.eventId = eventId;
        this.jobId = jobId;
        this.organizationId = organizationId;
        this.scheduledAt = scheduledAt;
        this.triggeredAt = triggeredAt;
        this.priority = priority;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
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

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public JobPriority getPriority() {
        return priority;
    }

    public void setPriority(JobPriority priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobTriggeredEvent that = (JobTriggeredEvent) o;
        return Objects.equals(eventId, that.eventId) &&
                Objects.equals(jobId, that.jobId) &&
                Objects.equals(organizationId, that.organizationId) &&
                Objects.equals(scheduledAt, that.scheduledAt) &&
                Objects.equals(triggeredAt, that.triggeredAt) &&
                priority == that.priority;
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, jobId, organizationId, scheduledAt, triggeredAt, priority);
    }

    @Override
    public String toString() {
        return "JobTriggeredEvent{" +
                "eventId=" + eventId +
                ", jobId=" + jobId +
                ", organizationId=" + organizationId +
                ", scheduledAt=" + scheduledAt +
                ", triggeredAt=" + triggeredAt +
                ", priority=" + priority +
                '}';
    }
}
