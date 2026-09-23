package com.chronos.tests.load.metrics;

import com.chronos.tests.common.TestContext;
import com.chronos.tests.common.TestHelper;
import com.chronos.tests.load.config.BenchmarkConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PipelineMetricsCollector {

    public static class ExecutionSnapshot {
        public final int completed;
        public final int failed;
        public final int retrying;
        public final int pending;
        public final int dlq;
        public final int total;
        public final double currentRate; // executions/sec in recent window
        public final long elapsedMs;

        public ExecutionSnapshot(int completed, int failed, int retrying, int pending, int dlq,
                                 int total, double currentRate, long elapsedMs) {
            this.completed = completed;
            this.failed = failed;
            this.retrying = retrying;
            this.pending = pending;
            this.dlq = dlq;
            this.total = total;
            this.currentRate = currentRate;
            this.elapsedMs = elapsedMs;
        }
    }

    public static class FinalBenchmarkMetrics {
        public final int jobsRequested;
        public final int jobsCreated;
        public final int jobApiErrors;
        public final double jobCreationThroughput;
        public final long jobCreationAvgLatencyMs;
        public final long jobCreationP95LatencyMs;

        public final int executionsRequested;
        public final int executionsCompleted;
        public final int executionsFailed;
        public final int executionsRetried;
        public final int executionsDlq;
        public final int executionsPending;

        public final double successRate;
        public final double errorRate;
        public final long totalDurationMs;
        public final double throughputExecPerSec;
        public final double peakRateExecPerSec;

        public final long minExecutionLatencyMs;
        public final long maxExecutionLatencyMs;
        public final double avgExecutionLatencyMs;
        public final long p50ExecutionLatencyMs;
        public final long p95ExecutionLatencyMs;
        public final long p99ExecutionLatencyMs;

        public final Map<String, Integer> workerDistribution;
        public final List<String> activeWorkersDetected;
        public final Map<String, Double> prometheusDelta;

        public FinalBenchmarkMetrics(int jobsRequested, int jobsCreated, int jobApiErrors,
                                     double jobCreationThroughput, long jobCreationAvgLatencyMs, long jobCreationP95LatencyMs,
                                     int executionsRequested, int executionsCompleted, int executionsFailed,
                                     int executionsRetried, int executionsDlq, int executionsPending,
                                     double successRate, double errorRate, long totalDurationMs,
                                     double throughputExecPerSec, double peakRateExecPerSec,
                                     long minExecutionLatencyMs, long maxExecutionLatencyMs,
                                     double avgExecutionLatencyMs, long p50ExecutionLatencyMs,
                                     long p95ExecutionLatencyMs, long p99ExecutionLatencyMs,
                                     Map<String, Integer> workerDistribution,
                                     List<String> activeWorkersDetected,
                                     Map<String, Double> prometheusDelta) {
            this.jobsRequested = jobsRequested;
            this.jobsCreated = jobsCreated;
            this.jobApiErrors = jobApiErrors;
            this.jobCreationThroughput = jobCreationThroughput;
            this.jobCreationAvgLatencyMs = jobCreationAvgLatencyMs;
            this.jobCreationP95LatencyMs = jobCreationP95LatencyMs;
            this.executionsRequested = executionsRequested;
            this.executionsCompleted = executionsCompleted;
            this.executionsFailed = executionsFailed;
            this.executionsRetried = executionsRetried;
            this.executionsDlq = executionsDlq;
            this.executionsPending = executionsPending;
            this.successRate = successRate;
            this.errorRate = errorRate;
            this.totalDurationMs = totalDurationMs;
            this.throughputExecPerSec = throughputExecPerSec;
            this.peakRateExecPerSec = peakRateExecPerSec;
            this.minExecutionLatencyMs = minExecutionLatencyMs;
            this.maxExecutionLatencyMs = maxExecutionLatencyMs;
            this.avgExecutionLatencyMs = avgExecutionLatencyMs;
            this.p50ExecutionLatencyMs = p50ExecutionLatencyMs;
            this.p95ExecutionLatencyMs = p95ExecutionLatencyMs;
            this.p99ExecutionLatencyMs = p99ExecutionLatencyMs;
            this.workerDistribution = workerDistribution;
            this.activeWorkersDetected = activeWorkersDetected;
            this.prometheusDelta = prometheusDelta;
        }
    }

    private final BenchmarkConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, Double> initialPrometheusMetrics = new HashMap<>();

    public PipelineMetricsCollector(BenchmarkConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public void captureInitialPrometheusMetrics() {
        initialPrometheusMetrics.clear();
        String execPromUrl = config.getExecutionServiceUrl() + "/actuator/prometheus";
        initialPrometheusMetrics.putAll(TestHelper.scrapePrometheusMetrics(execPromUrl));
        for (int p = 8085; p <= 8088; p++) {
            try {
                String workerPromUrl = "http://localhost:" + p + "/actuator/prometheus";
                initialPrometheusMetrics.putAll(TestHelper.scrapePrometheusMetrics(workerPromUrl));
            } catch (Exception ignored) {}
        }
    }

    public List<String> detectActiveWorkers() {
        List<String> workers = new ArrayList<>();
        int[] ports = {8085, 8086, 8087, 8088};
        for (int p : ports) {
            try {
                String workerHealthUrl = "http://localhost:" + p + "/actuator/health";
                HttpResponse<String> res = TestHelper.sendGet(workerHealthUrl);
                if (res.statusCode() == 200) {
                    workers.add("worker-local-" + (p - 8084));
                }
            } catch (Exception ignored) {}
        }
        if (workers.isEmpty()) {
            workers.add("worker-local-1");
        }
        return workers;
    }

    public FinalBenchmarkMetrics trackUntilCompletion(
            int jobsRequested,
            int jobsCreated,
            int jobApiErrors,
            double jobCreationThroughput,
            long jobCreationAvgLatencyMs,
            long jobCreationP95LatencyMs,
            int executionsRequested,
            Map<UUID, Instant> dispatchTimestamps,
            Consumer<ExecutionSnapshot> progressListener) {

        long startTimeMs = System.currentTimeMillis();
        long maxWaitTimeMs = config.getTimeoutSeconds() * 1000L;
        long lastSampleTimeMs = startTimeMs;
        int lastCompletedCount = 0;
        double peakRate = 0.0;

        List<ExecutionRecord> recordedExecutions = new ArrayList<>();
        int completedCount = 0;
        int failedCount = 0;
        int retryCount = 0;
        int pendingCount = 0;
        int dlqCount = 0;

        while (true) {
            long nowMs = System.currentTimeMillis();
            long elapsedTotalMs = nowMs - startTimeMs;

            recordedExecutions = fetchExecutionRecords();

            completedCount = 0;
            failedCount = 0;
            retryCount = 0;
            pendingCount = 0;
            dlqCount = 0;

            for (ExecutionRecord rec : recordedExecutions) {
                if ("SUCCEEDED".equalsIgnoreCase(rec.status)) {
                    completedCount++;
                } else if ("FAILED".equalsIgnoreCase(rec.status)) {
                    failedCount++;
                } else if ("RETRY_SCHEDULED".equalsIgnoreCase(rec.status)) {
                    retryCount++;
                } else if ("DEAD_LETTERED".equalsIgnoreCase(rec.status)) {
                    dlqCount++;
                } else {
                    pendingCount++;
                }
            }

            // Calculate instantaneous rate
            long sampleWindowMs = Math.max(1, nowMs - lastSampleTimeMs);
            int deltaCompleted = Math.max(0, completedCount - lastCompletedCount);
            double currentRate = (deltaCompleted / (double) sampleWindowMs) * 1000.0;
            if (currentRate > peakRate) {
                peakRate = currentRate;
            }

            if (sampleWindowMs >= 1000) {
                lastSampleTimeMs = nowMs;
                lastCompletedCount = completedCount;
            }

            if (progressListener != null) {
                ExecutionSnapshot snapshot = new ExecutionSnapshot(
                        completedCount, failedCount, retryCount, pendingCount, dlqCount,
                        recordedExecutions.size(), currentRate, elapsedTotalMs
                );
                progressListener.accept(snapshot);
            }

            // Check completion conditions
            int terminalCount = completedCount + failedCount + dlqCount;
            if (terminalCount >= executionsRequested && executionsRequested > 0) {
                // Workload complete
                break;
            }

            if (elapsedTotalMs >= maxWaitTimeMs) {
                System.out.println("\n[WARN] Benchmark reached maximum timeout of " + config.getTimeoutSeconds() + "s");
                break;
            }

            try {
                Thread.sleep(config.getPollIntervalMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        long totalDurationMs = Math.max(1, System.currentTimeMillis() - startTimeMs);
        double durationSec = totalDurationMs / 1000.0;

        double throughput = (completedCount + failedCount) / durationSec;
        double totalProcessed = completedCount + failedCount + dlqCount;
        double successRate = totalProcessed > 0 ? (completedCount / totalProcessed) * 100.0 : 0.0;
        double errorRate = totalProcessed > 0 ? ((failedCount + dlqCount) / totalProcessed) * 100.0 : (jobApiErrors > 0 ? 100.0 : 0.0);

        // Compute Latency Percentiles from actual completed execution timestamps
        List<Long> latencies = new ArrayList<>();
        Map<String, Integer> workerDist = new HashMap<>();

        for (ExecutionRecord rec : recordedExecutions) {
            if (rec.workerId != null && !rec.workerId.isBlank()) {
                workerDist.put(rec.workerId, workerDist.getOrDefault(rec.workerId, 0) + 1);
            }
            if (rec.durationMs > 0) {
                latencies.add(rec.durationMs);
            } else if (rec.createdAt != null && rec.completedAt != null) {
                long dur = Duration.between(rec.createdAt, rec.completedAt).toMillis();
                latencies.add(Math.max(1, dur));
            }
        }

        long minLat = 0;
        long maxLat = 0;
        double avgLat = 0.0;
        long p50Lat = 0;
        long p95Lat = 0;
        long p99Lat = 0;

        if (!latencies.isEmpty()) {
            latencies.sort(Long::compareTo);
            minLat = latencies.get(0);
            maxLat = latencies.get(latencies.size() - 1);
            avgLat = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
            int p50Idx = (int) Math.floor(0.50 * latencies.size());
            int p95Idx = (int) Math.floor(0.95 * latencies.size());
            int p99Idx = (int) Math.floor(0.99 * latencies.size());
            p50Lat = latencies.get(Math.min(p50Idx, latencies.size() - 1));
            p95Lat = latencies.get(Math.min(p95Idx, latencies.size() - 1));
            p99Lat = latencies.get(Math.min(p99Idx, latencies.size() - 1));
        }

        // Capture Prometheus Deltas
        Map<String, Double> finalPromMetrics = new HashMap<>();
        String execPromUrl = config.getExecutionServiceUrl() + "/actuator/prometheus";
        finalPromMetrics.putAll(TestHelper.scrapePrometheusMetrics(execPromUrl));
        for (int p = 8085; p <= 8088; p++) {
            try {
                String workerPromUrl = "http://localhost:" + p + "/actuator/prometheus";
                finalPromMetrics.putAll(TestHelper.scrapePrometheusMetrics(workerPromUrl));
            } catch (Exception ignored) {}
        }

        Map<String, Double> deltaMetrics = new HashMap<>();
        for (Map.Entry<String, Double> entry : finalPromMetrics.entrySet()) {
            double initial = initialPrometheusMetrics.getOrDefault(entry.getKey(), 0.0);
            double delta = entry.getValue() - initial;
            if (delta > 0) {
                deltaMetrics.put(entry.getKey(), delta);
            }
        }

        List<String> activeWorkers = detectActiveWorkers();

        return new FinalBenchmarkMetrics(
                jobsRequested, jobsCreated, jobApiErrors,
                jobCreationThroughput, jobCreationAvgLatencyMs, jobCreationP95LatencyMs,
                executionsRequested, completedCount, failedCount,
                retryCount, dlqCount, pendingCount,
                successRate, errorRate, totalDurationMs,
                throughput, Math.max(peakRate, throughput),
                minLat, maxLat, avgLat, p50Lat, p95Lat, p99Lat,
                workerDist, activeWorkers, deltaMetrics
        );
    }

    private static class ExecutionRecord {
        String id;
        String status;
        String workerId;
        int attempt;
        Instant createdAt;
        Instant completedAt;
        long durationMs;
    }

    private List<ExecutionRecord> fetchExecutionRecords() {
        List<ExecutionRecord> records = new ArrayList<>();

        // 1. Try fetching via API Gateway
        if (config.isUseGateway() && config.getGatewayUrl() != null) {
            String url = config.getGatewayUrl() + "/api/v1/executions";
            try {
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("X-Organization-Id", config.getOrganizationId().toString())
                        .timeout(Duration.ofSeconds(5))
                        .GET();

                if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
                    reqBuilder.header("X-API-Key", config.getApiKey());
                } else if (config.getJwtToken() != null && !config.getJwtToken().isBlank()) {
                    reqBuilder.header("Authorization", "Bearer " + config.getJwtToken());
                }

                HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    List<ExecutionRecord> parsed = parseExecutionRecords(response.body());
                    if (!parsed.isEmpty()) {
                        return parsed;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. Try direct Execution Service REST API
        if (config.getExecutionServiceUrl() != null) {
            String directUrl = config.getExecutionServiceUrl() + "/api/v1/executions";
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(directUrl))
                        .header("X-Organization-Id", config.getOrganizationId().toString())
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    List<ExecutionRecord> parsed = parseExecutionRecords(response.body());
                    if (!parsed.isEmpty()) {
                        return parsed;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3. Fallback: Direct Database query for this organizationId
        try (Connection conn = DriverManager.getConnection(TestContext.DB_URL_EXECUTION, TestContext.DB_USER, TestContext.DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, status, worker_id, attempt, created_at, completed_at FROM executions WHERE organization_id = ?")) {
            ps.setObject(1, config.getOrganizationId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ExecutionRecord r = new ExecutionRecord();
                    r.id = rs.getString("id");
                    r.status = rs.getString("status");
                    r.workerId = rs.getString("worker_id");
                    r.attempt = rs.getInt("attempt");
                    java.sql.Timestamp cTs = rs.getTimestamp("created_at");
                    java.sql.Timestamp compTs = rs.getTimestamp("completed_at");
                    if (cTs != null) r.createdAt = cTs.toInstant();
                    if (compTs != null) r.completedAt = compTs.toInstant();
                    if (r.createdAt != null && r.completedAt != null) {
                        r.durationMs = Math.max(1, Duration.between(r.createdAt, r.completedAt).toMillis());
                    }
                    records.add(r);
                }
            }
        } catch (Exception ignored) {}

        return records;
    }

    private List<ExecutionRecord> parseExecutionRecords(String json) {
        List<ExecutionRecord> list = new ArrayList<>();
        try {
            JsonNode array = objectMapper.readTree(json);
            if (array.isArray()) {
                for (JsonNode item : array) {
                    ExecutionRecord r = new ExecutionRecord();
                    r.id = item.has("id") ? item.get("id").asText() : null;
                    r.status = item.has("status") ? item.get("status").asText() : "UNKNOWN";
                    r.workerId = item.has("workerId") && !item.get("workerId").isNull() ? item.get("workerId").asText() : null;
                    r.attempt = item.has("attempt") ? item.get("attempt").asInt() : 1;
                    if (item.has("createdAt") && !item.get("createdAt").isNull()) {
                        try { r.createdAt = Instant.parse(item.get("createdAt").asText()); } catch (Exception ignored) {}
                    }
                    if (item.has("completedAt") && !item.get("completedAt").isNull()) {
                        try { r.completedAt = Instant.parse(item.get("completedAt").asText()); } catch (Exception ignored) {}
                    }
                    if (r.createdAt != null && r.completedAt != null) {
                        r.durationMs = Math.max(1, Duration.between(r.createdAt, r.completedAt).toMillis());
                    }
                    list.add(r);
                }
            }
        } catch (Exception ignored) {}
        return list;
    }
}
