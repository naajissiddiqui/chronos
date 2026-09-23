package com.chronos.tests.load.reporter;

import com.chronos.tests.load.config.BenchmarkConfig;
import com.chronos.tests.load.metrics.PipelineMetricsCollector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.FileWriter;
import java.time.Duration;
import java.util.Map;

public class BenchmarkReporter {

    private final BenchmarkConfig config;
    private final ObjectMapper objectMapper;

    public BenchmarkReporter(BenchmarkConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static void renderProgressBar(String taskName, int current, int total) {
        int width = 25;
        double progress = total > 0 ? (double) current / total : 0.0;
        int filled = (int) Math.round(progress * width);

        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for (int i = 0; i < width; i++) {
            if (i < filled) {
                bar.append("=");
            } else if (i == filled) {
                bar.append(">");
            } else {
                bar.append(" ");
            }
        }
        bar.append("]");

        int percent = (int) Math.round(progress * 100.0);
        System.out.printf("\r%-20s %s %4d/%-4d (%3d%%)", taskName, bar.toString(), current, total, percent);
        if (current >= total) {
            System.out.println();
        }
    }

    public void printTerminalReport(PipelineMetricsCollector.FinalBenchmarkMetrics m) {
        System.out.println("\n========================================");
        System.out.println("CHRONOS LOAD TEST & BENCHMARK REPORT");
        System.out.println("========================================");
        System.out.println();
        System.out.println("Configuration");
        System.out.println("-------------");
        System.out.printf("%-24s %s%n", "Run ID:", config.getRunId());
        System.out.printf("%-24s %s%n", "Scenario:", config.getScenario().name());
        System.out.printf("%-24s %s%n", "Trigger Pipeline:",
                config.getTriggerMode() == BenchmarkConfig.PipelineTriggerMode.SCHEDULER ?
                        "SCHEDULER (True E2E: Job Service -> Outbox -> Scheduler -> Kafka -> Execution -> Worker)" :
                        "KAFKA_DIRECT (Direct Kafka Injection)");
        System.out.printf("%-24s %d%n", "Jobs requested:", m.jobsRequested);
        System.out.printf("%-24s %d%n", "Executions requested:", m.executionsRequested);
        System.out.printf("%-24s %.1f%%%n", "Target Failure Rate:", config.getFailureRate() * 100.0);
        System.out.printf("%-24s %s%n", "Active Workers:", String.join(", ", m.activeWorkersDetected));
        System.out.printf("%-24s %s%n", "Environment:", "localhost");
        System.out.println();
        System.out.println("Run-Level Benchmark Results (Tenant Isolated)");
        System.out.println("---------------------------------------------");
        System.out.printf("%-24s %d (API errors: %d)%n", "Jobs created:", m.jobsCreated, m.jobApiErrors);
        System.out.printf("%-24s %d%n", "Executions completed:", m.executionsCompleted);
        System.out.printf("%-24s %d%n", "Executions failed:", m.executionsFailed);
        System.out.printf("%-24s %d%n", "Executions retried:", m.executionsRetried);
        System.out.printf("%-24s %d%n", "Executions DLQ:", m.executionsDlq);
        System.out.printf("%-24s %d%n", "Executions pending:", m.executionsPending);
        System.out.println();
        System.out.printf("%-24s %6.2f%%%n", "Success rate:", m.successRate);
        System.out.printf("%-24s %6.2f%%%n", "Error rate:", m.errorRate);
        System.out.println();
        System.out.printf("%-24s %s (%.2f s)%n", "Duration:", formatDuration(m.totalDurationMs), m.totalDurationMs / 1000.0);
        System.out.printf("%-24s %6.2f executions/sec%n", "Throughput:", m.throughputExecPerSec);
        System.out.printf("%-24s %6.2f executions/sec%n", "Peak Processing Rate:", m.peakRateExecPerSec);
        System.out.printf("%-24s %6.2f jobs/sec%n", "Job Creation Rate:", m.jobCreationThroughput);
        System.out.println();
        System.out.println("Execution Latency (Dispatch -> Completed)");
        System.out.println("------------------------------------------");
        if (m.executionsCompleted + m.executionsFailed > 0) {
            System.out.printf("%-24s %d ms%n", "p50:", m.p50ExecutionLatencyMs);
            System.out.printf("%-24s %d ms%n", "p95:", m.p95ExecutionLatencyMs);
            System.out.printf("%-24s %d ms%n", "p99:", m.p99ExecutionLatencyMs);
            System.out.printf("%-24s %d ms%n", "Min:", m.minExecutionLatencyMs);
            System.out.printf("%-24s %d ms%n", "Max:", m.maxExecutionLatencyMs);
            System.out.printf("%-24s %.2f ms%n", "Avg:", m.avgExecutionLatencyMs);
        } else {
            System.out.println("N/A - metric unavailable (no executions completed)");
        }
        System.out.println();
        System.out.println("HTTP / API Latency (Job Service via Gateway)");
        System.out.println("--------------------------------------------");
        if (m.jobsCreated > 0) {
            System.out.printf("%-24s %d ms%n", "Job Creation Avg:", m.jobCreationAvgLatencyMs);
            System.out.printf("%-24s %d ms%n", "Job Creation p95:", m.jobCreationP95LatencyMs);
        } else {
            System.out.println("N/A - metric unavailable (0 jobs created)");
        }
        System.out.println();

        if (m.workerDistribution != null && !m.workerDistribution.isEmpty()) {
            System.out.println("Worker Distribution");
            System.out.println("-------------------");
            int totalProcessed = Math.max(1, m.executionsCompleted + m.executionsFailed);
            for (Map.Entry<String, Integer> entry : m.workerDistribution.entrySet()) {
                double pct = (entry.getValue() / (double) totalProcessed) * 100.0;
                System.out.printf("%-24s %d executions (%.1f%%)%n", entry.getKey() + ":", entry.getValue(), pct);
            }
            System.out.println();
        }

        if (m.prometheusDelta != null && !m.prometheusDelta.isEmpty()) {
            System.out.println("Global Service Prometheus Counter Deltas (Cluster-Wide Activity During Test Window)");
            System.out.println("----------------------------------------------------------------------------------");
            System.out.println("[Note: These represent global JVM-level Micrometer counter increases across the entire cluster during the test window, distinct from the isolated benchmark run results above]");
            for (Map.Entry<String, Double> entry : m.prometheusDelta.entrySet()) {
                System.out.printf("%-40s +%.0f%n", entry.getKey() + ":", entry.getValue());
            }
            System.out.println();
        }

        System.out.println("========================================\n");

        saveJsonReport(m);
    }

    private void saveJsonReport(PipelineMetricsCollector.FinalBenchmarkMetrics m) {
        try {
            File dir = new File("target/load-test-reports");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "benchmark-" + config.getRunId() + ".json");
            try (FileWriter writer = new FileWriter(file)) {
                objectMapper.writeValue(writer, m);
            }
            System.out.println("[INFO] Benchmark report JSON saved to: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[WARN] Failed to write JSON report: " + e.getMessage());
        }
    }

    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long mins = seconds / 60;
        long remainingSecs = seconds % 60;
        if (mins > 0) {
            return String.format("%dm %02ds", mins, remainingSecs);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
