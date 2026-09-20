package com.chronos.tests.load.cleanup;

import com.chronos.tests.common.TestContext;
import com.chronos.tests.load.config.BenchmarkConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class BenchmarkCleaner {

    private final BenchmarkConfig config;
    private final HttpClient httpClient;

    public BenchmarkCleaner(BenchmarkConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void cleanup(List<UUID> createdJobIds) {
        if (!config.isCleanup()) {
            return;
        }

        System.out.println("\n[CLEANUP] Cleaning up test resources for Run ID: " + config.getRunId());

        int deletedExecutions = 0;
        int deletedJobs = 0;

        // 1. Direct database cleanup by isolated Organization ID
        try (Connection conn = DriverManager.getConnection(TestContext.DB_URL_JOB, TestContext.DB_USER, TestContext.DB_PASS)) {
            // Delete executions for this tenant
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM executions WHERE organization_id = ?")) {
                ps.setObject(1, config.getOrganizationId());
                deletedExecutions = ps.executeUpdate();
            }

            // Delete jobs for this tenant
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM jobs WHERE organization_id = ?")) {
                ps.setObject(1, config.getOrganizationId());
                deletedJobs = ps.executeUpdate();
            }

            System.out.println("[CLEANUP] Database: Safely deleted " + deletedJobs + " job(s) and " + deletedExecutions + " execution(s).");
            return;
        } catch (Exception e) {
            System.out.println("[CLEANUP] Direct DB cleanup note: " + e.getMessage() + ". Attempting REST API cleanup.");
        }

        // 2. Fallback: REST API deletion by job IDs
        if (createdJobIds != null && !createdJobIds.isEmpty()) {
            String baseUrl = (config.isUseGateway() ? config.getGatewayUrl() : config.getJobServiceUrl()) + "/api/v1/jobs/";
            for (UUID jobId : createdJobIds) {
                try {
                    HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                            .uri(URI.create(baseUrl + jobId))
                            .header("X-Organization-Id", config.getOrganizationId().toString())
                            .timeout(Duration.ofSeconds(5))
                            .DELETE();

                    if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
                        reqBuilder.header("X-API-Key", config.getApiKey());
                    } else if (config.getJwtToken() != null && !config.getJwtToken().isBlank()) {
                        reqBuilder.header("Authorization", "Bearer " + config.getJwtToken());
                    }

                    HttpResponse<String> res = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
                    if (res.statusCode() == 200 || res.statusCode() == 204) {
                        deletedJobs++;
                    }
                } catch (Exception ignored) {}
            }
            System.out.println("[CLEANUP] REST API: Deleted " + deletedJobs + " job(s).");
        }
    }
}
