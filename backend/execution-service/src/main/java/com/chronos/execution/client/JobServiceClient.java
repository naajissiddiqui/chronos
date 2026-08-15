package com.chronos.execution.client;

import com.chronos.execution.dto.JobRetryConfigDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class JobServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(JobServiceClient.class);

    private final RestTemplate restTemplate;
    private final String jobServiceBaseUrl;

    public JobServiceClient(
            @Value("${job-service.url:http://localhost:8082}") String jobServiceBaseUrl) {
        this.restTemplate = new RestTemplate();
        this.jobServiceBaseUrl = jobServiceBaseUrl;
    }

    public JobRetryConfigDto getJobRetryConfig(UUID jobId, UUID organizationId) {
        if (jobId == null) {
            return new JobRetryConfigDto(3, 10);
        }

        try {
            String url = jobServiceBaseUrl + "/api/v1/jobs/" + jobId;
            HttpHeaders headers = new HttpHeaders();
            if (organizationId != null) {
                headers.set("X-Organization-Id", organizationId.toString());
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<JobRetryConfigDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    JobRetryConfigDto.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JobRetryConfigDto dto = response.getBody();
                int maxRetries = dto.getMaxRetries() != null ? dto.getMaxRetries() : 3;
                int backoff = dto.getRetryBackoffSeconds() != null ? dto.getRetryBackoffSeconds() : 10;
                return new JobRetryConfigDto(maxRetries, backoff);
            }
        } catch (Exception e) {
            logger.warn("Could not fetch job config from Job Service (jobId={}): {}. Falling back to defaults (maxRetries=3, backoff=10s)",
                    jobId, e.getMessage());
        }

        return new JobRetryConfigDto(3, 10);
    }
}
