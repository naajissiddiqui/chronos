package com.chronos.execution.dto;

public class JobRetryConfigDto {

    private Integer maxRetries;
    private Integer retryBackoffSeconds;

    public JobRetryConfigDto() {
    }

    public JobRetryConfigDto(Integer maxRetries, Integer retryBackoffSeconds) {
        this.maxRetries = maxRetries;
        this.retryBackoffSeconds = retryBackoffSeconds;
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
