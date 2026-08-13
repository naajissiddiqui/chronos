package com.chronos.job.dto;

import com.chronos.job.entity.Job;
import com.chronos.job.entity.JobPriority;
import com.chronos.job.entity.JobStatus;

import java.time.Instant;
import java.util.UUID;

public class JobResponse {

    private UUID id;
    private UUID organizationId;
    private String name;
    private String description;
    private JobStatus status;
    private String schedule;
    private String timezone;
    private boolean enabled;
    private JobPriority priority;
    private Integer timeoutSeconds;
    private Integer maxRetries;
    private Integer retryBackoffSeconds;
    private Instant nextRunAt;
    private Instant createdAt;
    private Instant updatedAt;

    public JobResponse() {
    }

    public static JobResponse fromEntity(Job job) {
        JobResponse response = new JobResponse();
        response.setId(job.getId());
        response.setOrganizationId(job.getOrganizationId());
        response.setName(job.getName());
        response.setDescription(job.getDescription());
        response.setStatus(job.getStatus());
        response.setSchedule(job.getSchedule());
        response.setTimezone(job.getTimezone());
        response.setEnabled(job.isEnabled());
        response.setPriority(job.getPriority());
        response.setTimeoutSeconds(job.getTimeoutSeconds());
        response.setMaxRetries(job.getMaxRetries());
        response.setRetryBackoffSeconds(job.getRetryBackoffSeconds());
        response.setNextRunAt(job.getNextRunAt());
        response.setCreatedAt(job.getCreatedAt());
        response.setUpdatedAt(job.getUpdatedAt());
        return response;
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public JobPriority getPriority() {
        return priority;
    }

    public void setPriority(JobPriority priority) {
        this.priority = priority;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Integer getRetryBackoffSeconds() {
        return retryBackoffSeconds;
    }

    public void setRetryBackoffSeconds(Integer retryBackoffSeconds) {
        this.retryBackoffSeconds = retryBackoffSeconds;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(Instant nextRunAt) {
        this.nextRunAt = nextRunAt;
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
}
