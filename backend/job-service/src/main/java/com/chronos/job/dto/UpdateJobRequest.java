package com.chronos.job.dto;

import com.chronos.job.entity.JobPriority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateJobRequest {

    @NotBlank(message = "Job name is required")
    @Size(max = 100, message = "Job name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotBlank(message = "Schedule (cron expression) is required")
    private String schedule;

    @NotBlank(message = "Timezone is required")
    private String timezone;

    @NotNull(message = "Priority is required")
    private JobPriority priority;

    @NotNull(message = "Timeout in seconds is required")
    @Min(value = 1, message = "Timeout must be at least 1 second")
    private Integer timeoutSeconds;

    @NotNull(message = "Max retries is required")
    @Min(value = 0, message = "Max retries must be non-negative")
    private Integer maxRetries;

    @NotNull(message = "Retry backoff in seconds is required")
    @Min(value = 0, message = "Retry backoff must be non-negative")
    private Integer retryBackoffSeconds;

    public UpdateJobRequest() {
    }

    public UpdateJobRequest(String name, String description, String schedule, String timezone,
                            JobPriority priority, Integer timeoutSeconds, Integer maxRetries,
                            Integer retryBackoffSeconds) {
        this.name = name;
        this.description = description;
        this.schedule = schedule;
        this.timezone = timezone;
        this.priority = priority;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = maxRetries;
        this.retryBackoffSeconds = retryBackoffSeconds;
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
}
