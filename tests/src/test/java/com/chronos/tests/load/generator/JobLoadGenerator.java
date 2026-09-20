package com.chronos.tests.load.generator;

import com.chronos.tests.load.config.BenchmarkConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class JobLoadGenerator {

    public static class JobCreationResult {
        private final int requested;
        private final int created;
        private final int failed;
        private final long durationMs;
        private final double throughputJobsPerSec;
        private final List<Long> latenciesMs;
        private final List<UUID> createdJobIds;
        private final List<String> errors;

        public JobCreationResult(int requested, int created, int failed, long durationMs,
                                 double throughputJobsPerSec, List<Long> latenciesMs,
                                 List<UUID> createdJobIds, List<String> errors) {
            this.requested = requested;
            this.created = created;
            this.failed = failed;
            this.durationMs = durationMs;
            this.throughputJobsPerSec = throughputJobsPerSec;
            this.latenciesMs = latenciesMs;
            this.createdJobIds = createdJobIds;
            this.errors = errors;
        }

        public int getRequested() { return requested; }
        public int getCreated() { return created; }
        public int getFailed() { return failed; }
        public long getDurationMs() { return durationMs; }
        public double getThroughputJobsPerSec() { return throughputJobsPerSec; }
        public List<Long> getLatenciesMs() { return latenciesMs; }
        public List<UUID> getCreatedJobIds() { return createdJobIds; }
        public List<String> getErrors() { return errors; }

        public long getMinLatency() {
            return latenciesMs.isEmpty() ? 0 : latenciesMs.get(0);
        }

        public long getMaxLatency() {
            return latenciesMs.isEmpty() ? 0 : latenciesMs.get(latenciesMs.size() - 1);
        }

        public double getAvgLatency() {
            return latenciesMs.isEmpty() ? 0.0 : latenciesMs.stream().mapToLong(Long::longValue).average().orElse(0.0);
        }

        public long getP50Latency() {
            if (latenciesMs.isEmpty()) return 0;
            int idx = (int) Math.floor(0.50 * latenciesMs.size());
            return latenciesMs.get(Math.min(idx, latenciesMs.size() - 1));
        }

        public long getP95Latency() {
            if (latenciesMs.isEmpty()) return 0;
            int idx = (int) Math.floor(0.95 * latenciesMs.size());
            return latenciesMs.get(Math.min(idx, latenciesMs.size() - 1));
        }

        public long getP99Latency() {
            if (latenciesMs.isEmpty()) return 0;
            int idx = (int) Math.floor(0.99 * latenciesMs.size());
            return latenciesMs.get(Math.min(idx, latenciesMs.size() - 1));
        }
    }

    private final BenchmarkConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public JobLoadGenerator(BenchmarkConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public boolean authenticate() {
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            return true;
        }
        if (config.getJwtToken() != null && !config.getJwtToken().isBlank()) {
            return true;
        }

        // Try registering a benchmark user to obtain a JWT token
        String email = "bench-" + config.getOrganizationId().toString().substring(0, 8) + "@chronos.local";
        String password = "BenchmarkPassword123!";
        String orgName = "Benchmark Org " + config.getRunId();

        try {
            Map<String, Object> regBody = Map.of(
                    "email", email,
                    "password", password,
                    "firstName", "Benchmark",
                    "lastName", "Runner",
                    "organizationName", orgName,
                    "role", "OWNER"
            );
            String json = objectMapper.writeValueAsString(regBody);

            String authUrl = (config.isUseGateway() ? config.getGatewayUrl() : config.getAuthServiceUrl()) + "/api/v1/auth/register";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(authUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                JsonNode node = objectMapper.readTree(response.body());
                if (node.has("accessToken")) {
                    config.setJwtToken(node.get("accessToken").asText());
                } else if (node.has("token")) {
                    config.setJwtToken(node.get("token").asText());
                }

                if (node.has("user") && node.get("user").has("organizationId")) {
                    try {
                        config.setOrganizationId(UUID.fromString(node.get("user").get("organizationId").asText()));
                    } catch (Exception ignored) {}
                } else if (node.has("organizationId")) {
                    try {
                        config.setOrganizationId(UUID.fromString(node.get("organizationId").asText()));
                    } catch (Exception ignored) {}
                }
                return true;
            } else if (response.statusCode() == 409 || response.statusCode() == 400) {
                // User already exists or error, try logging in
                Map<String, String> loginBody = Map.of("email", email, "password", password);
                String loginJson = objectMapper.writeValueAsString(loginBody);
                String loginUrl = (config.isUseGateway() ? config.getGatewayUrl() : config.getAuthServiceUrl()) + "/api/v1/auth/login";
                HttpRequest loginReq = HttpRequest.newBuilder()
                        .uri(URI.create(loginUrl))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                        .build();

                HttpResponse<String> loginRes = httpClient.send(loginReq, HttpResponse.BodyHandlers.ofString());
                if (loginRes.statusCode() == 200) {
                    JsonNode node = objectMapper.readTree(loginRes.body());
                    if (node.has("accessToken")) {
                        config.setJwtToken(node.get("accessToken").asText());
                    } else if (node.has("token")) {
                        config.setJwtToken(node.get("token").asText());
                    }

                    if (node.has("user") && node.get("user").has("organizationId")) {
                        try {
                            config.setOrganizationId(UUID.fromString(node.get("user").get("organizationId").asText()));
                        } catch (Exception ignored) {}
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Authentication registration skipped/failed: " + e.getMessage());
        }

        return false;
    }

    public JobCreationResult generateJobs(BiConsumer<Integer, Integer> progressCallback) {
        authenticate();

        int targetJobs = config.getJobs();
        int concurrency = Math.min(config.getConcurrency(), targetJobs);
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);

        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        List<UUID> createdJobIds = Collections.synchronizedList(new ArrayList<>());
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        AtomicInteger createdCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        String targetUrl = (config.isUseGateway() ? config.getGatewayUrl() : config.getJobServiceUrl()) + "/api/v1/jobs";

        long startBenchmarkTime = System.currentTimeMillis();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 1; i <= targetJobs; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                boolean isFailureJob = (config.getFailureRate() > 0.0) && (Math.random() < config.getFailureRate());
                String priority = isFailureJob ? "CRITICAL" : "NORMAL";

                Map<String, Object> jobPayload = new HashMap<>();
                jobPayload.put("name", "LoadTest-" + config.getRunId() + "-Job-" + index);
                jobPayload.put("description", "Benchmark job #" + index + " for run " + config.getRunId() + (isFailureJob ? " [FAIL_INTENDED]" : ""));
                jobPayload.put("schedule", "* * * * * ?");
                jobPayload.put("timezone", "UTC");
                jobPayload.put("priority", priority);
                jobPayload.put("timeoutSeconds", 300);
                jobPayload.put("maxRetries", 3);
                jobPayload.put("retryBackoffSeconds", 2);

                try {
                    String jsonBody = objectMapper.writeValueAsString(jobPayload);
                    HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                            .uri(URI.create(targetUrl))
                            .header("Content-Type", "application/json")
                            .header("X-Organization-Id", config.getOrganizationId().toString())
                            .header("X-Run-Id", config.getRunId())
                            .timeout(Duration.ofSeconds(15))
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

                    if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
                        reqBuilder.header("X-API-Key", config.getApiKey());
                    } else if (config.getJwtToken() != null && !config.getJwtToken().isBlank()) {
                        reqBuilder.header("Authorization", "Bearer " + config.getJwtToken());
                    }

                    long reqStart = System.currentTimeMillis();
                    HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
                    long reqElapsed = System.currentTimeMillis() - reqStart;

                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        latencies.add(reqElapsed);
                        JsonNode node = objectMapper.readTree(response.body());
                        if (node.has("id")) {
                            createdJobIds.add(UUID.fromString(node.get("id").asText()));
                        }
                        int currCreated = createdCount.incrementAndGet();
                        if (progressCallback != null) {
                            progressCallback.accept(currCreated, targetJobs);
                        }
                    } else {
                        failedCount.incrementAndGet();
                        errors.add("HTTP " + response.statusCode() + ": " + response.body());
                    }
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                    errors.add("Exception: " + e.getMessage());
                }
            }, executor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        long totalDurationMs = Math.max(1, System.currentTimeMillis() - startBenchmarkTime);
        double durationSec = totalDurationMs / 1000.0;
        double throughput = createdCount.get() / durationSec;

        latencies.sort(Long::compareTo);

        return new JobCreationResult(
                targetJobs,
                createdCount.get(),
                failedCount.get(),
                totalDurationMs,
                throughput,
                latencies,
                createdJobIds,
                errors
        );
    }
}
