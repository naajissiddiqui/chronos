package com.chronos.tests.load;

import com.chronos.tests.load.cleanup.BenchmarkCleaner;
import com.chronos.tests.load.config.BenchmarkConfig;
import com.chronos.tests.load.generator.ExecutionPipelineTrigger;
import com.chronos.tests.load.generator.JobLoadGenerator;
import com.chronos.tests.load.metrics.PipelineMetricsCollector;
import com.chronos.tests.load.reporter.BenchmarkReporter;

import java.util.List;
import java.util.UUID;

public class ChronosBenchmarkRunner {

    public static PipelineMetricsCollector.FinalBenchmarkMetrics run(BenchmarkConfig config) {
        System.out.println("\n=======================================================");
        System.out.println("  CHRONOS DISTRIBUTED SYSTEM BENCHMARK & LOAD TEST");
        System.out.println("=======================================================");
        System.out.println("Mode:           " + config.getScenario().name());
        System.out.println("Run ID:         " + config.getRunId());
        System.out.println("Tenant Org ID:  " + config.getOrganizationId());
        System.out.println("Target Jobs:    " + config.getJobs());
        System.out.println("Target Execs:   " + config.getExecutions());
        System.out.println("Failure Rate:   " + (config.getFailureRate() * 100.0) + "%");
        System.out.println("Concurrency:    " + config.getConcurrency());
        System.out.println("Timeout:        " + config.getTimeoutSeconds() + "s");
        System.out.println("Cleanup:        " + (config.isCleanup() ? "ENABLED" : "DISABLED"));
        System.out.println("=======================================================\n");

        PipelineMetricsCollector metricsCollector = new PipelineMetricsCollector(config);
        metricsCollector.captureInitialPrometheusMetrics();

        // 1. Generate Jobs
        System.out.println("[Phase 1/3] Submitting Jobs via API Gateway / Job Service...");
        JobLoadGenerator jobGenerator = new JobLoadGenerator(config);
        JobLoadGenerator.JobCreationResult jobResult = jobGenerator.generateJobs((curr, total) -> {
            BenchmarkReporter.renderProgressBar("Creating Jobs", curr, total);
        });

        System.out.printf(" -> Jobs Created: %d/%d (API Errors: %d, Throughput: %.2f jobs/sec, Avg Latency: %dms)%n",
                jobResult.getCreated(), jobResult.getRequested(), jobResult.getFailed(),
                jobResult.getThroughputJobsPerSec(), jobResult.getAvgLatency() > 0 ? (long) jobResult.getAvgLatency() : 0);

        List<UUID> jobIds = jobResult.getCreatedJobIds();
        if (jobIds.isEmpty()) {
            // Create fallback UUIDs if services are in stub/offline mode
            for (int i = 0; i < config.getJobs(); i++) {
                jobIds.add(UUID.randomUUID());
            }
        }

        // 2. Dispatch Execution Workload
        System.out.println("\n[Phase 2/3] Dispatching Execution Pipeline Workload (Kafka job.triggered)...");
        ExecutionPipelineTrigger trigger = new ExecutionPipelineTrigger(config);
        ExecutionPipelineTrigger.TriggerResult triggerResult = trigger.triggerPipelineExecutions(jobIds, (curr, total) -> {
            BenchmarkReporter.renderProgressBar("Dispatching Execs", curr, total);
        });

        System.out.printf(" -> Workload Dispatched: %d executions generated%n", triggerResult.getDispatched());

        // 3. Track Execution Pipeline State
        System.out.println("\n[Phase 3/3] Tracking Execution Pipeline Progress...");
        PipelineMetricsCollector.FinalBenchmarkMetrics finalMetrics = metricsCollector.trackUntilCompletion(
                jobResult.getRequested(),
                jobResult.getCreated(),
                jobResult.getFailed(),
                jobResult.getThroughputJobsPerSec(),
                (long) jobResult.getAvgLatency(),
                jobResult.getP95Latency(),
                config.getExecutions(),
                triggerResult.getDispatchTimestamps(),
                snapshot -> {
                    System.out.printf("\r[PROGRESS] Completed: %-4d | Failed: %-3d | Retrying: %-3d | Pending: %-4d | Rate: %5.1f exec/s | Elapsed: %4.1fs",
                            snapshot.completed, snapshot.failed, snapshot.retrying, snapshot.pending,
                            snapshot.currentRate, snapshot.elapsedMs / 1000.0);
                }
        );
        System.out.println();

        // 4. Report Final Results
        BenchmarkReporter reporter = new BenchmarkReporter(config);
        reporter.printTerminalReport(finalMetrics);

        // 5. Cleanup if requested
        if (config.isCleanup()) {
            BenchmarkCleaner cleaner = new BenchmarkCleaner(config);
            cleaner.cleanup(jobIds);
        }

        return finalMetrics;
    }

    public static void main(String[] args) {
        BenchmarkConfig config = parseCommandLineArgs(args);
        try {
            run(config);
        } catch (Exception e) {
            System.err.println("\n[FATAL] Benchmark failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static BenchmarkConfig parseCommandLineArgs(String[] args) {
        BenchmarkConfig config = new BenchmarkConfig();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            switch (arg) {
                case "--mode":
                case "-m":
                    if (i + 1 < args.length) {
                        try {
                            BenchmarkConfig.Scenario s = BenchmarkConfig.Scenario.valueOf(args[++i].toUpperCase());
                            config.setScenario(s);
                            config.setJobs(s.getDefaultJobs());
                            config.setExecutions(s.getDefaultExecutions());
                            config.setFailureRate(s.getDefaultFailureRate());
                        } catch (IllegalArgumentException e) {
                            System.err.println("Unknown scenario: " + args[i] + ". Using custom.");
                        }
                    }
                    break;
                case "--jobs":
                case "-j":
                    if (i + 1 < args.length) config.setJobs(Integer.parseInt(args[++i]));
                    break;
                case "--executions":
                case "-e":
                    if (i + 1 < args.length) config.setExecutions(Integer.parseInt(args[++i]));
                    break;
                case "--concurrency":
                case "-c":
                    if (i + 1 < args.length) config.setConcurrency(Integer.parseInt(args[++i]));
                    break;
                case "--rate":
                case "-r":
                    if (i + 1 < args.length) config.setRateLimit(Integer.parseInt(args[++i]));
                    break;
                case "--failure-rate":
                case "-f":
                    if (i + 1 < args.length) config.setFailureRate(Double.parseDouble(args[++i]));
                    break;
                case "--timeout":
                case "-t":
                    if (i + 1 < args.length) {
                        String val = args[++i];
                        if (val.endsWith("m")) {
                            config.setTimeoutSeconds(Integer.parseInt(val.replace("m", "")) * 60);
                        } else if (val.endsWith("s")) {
                            config.setTimeoutSeconds(Integer.parseInt(val.replace("s", "")));
                        } else {
                            config.setTimeoutSeconds(Integer.parseInt(val));
                        }
                    }
                    break;
                case "--cleanup":
                    config.setCleanup(true);
                    break;
                case "--gateway-url":
                    if (i + 1 < args.length) config.setGatewayUrl(args[++i]);
                    break;
                case "--job-service-url":
                    if (i + 1 < args.length) config.setJobServiceUrl(args[++i]);
                    break;
                case "--execution-service-url":
                    if (i + 1 < args.length) config.setExecutionServiceUrl(args[++i]);
                    break;
                case "--worker-service-url":
                    if (i + 1 < args.length) config.setWorkerServiceUrl(args[++i]);
                    break;
                case "--kafka-bootstrap":
                    if (i + 1 < args.length) config.setKafkaBootstrapServers(args[++i]);
                    break;
                case "--redis-host":
                    if (i + 1 < args.length) config.setRedisHost(args[++i]);
                    break;
                case "--redis-port":
                    if (i + 1 < args.length) config.setRedisPort(Integer.parseInt(args[++i]));
                    break;
                case "--prometheus-url":
                    if (i + 1 < args.length) config.setPrometheusUrl(args[++i]);
                    break;
                case "--api-key":
                    if (i + 1 < args.length) config.setApiKey(args[++i]);
                    break;
                case "--no-gateway":
                    config.setUseGateway(false);
                    break;
                case "--help":
                case "-h":
                    printHelp();
                    System.exit(0);
                    break;
            }
        }

        return config;
    }

    private static void printHelp() {
        System.out.println("Chronos Load Testing & Benchmark CLI");
        System.out.println("Usage: java -cp ... com.chronos.tests.load.ChronosBenchmarkRunner [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --mode <SMALL|MEDIUM|LARGE|STRESS|FAILURE>  Benchmark scenario preset");
        System.out.println("  --jobs <N>                                 Number of jobs to create");
        System.out.println("  --executions <N>                           Number of executions to generate");
        System.out.println("  --concurrency <N>                          HTTP client worker thread concurrency (default: 10)");
        System.out.println("  --rate <N>                                 Execution trigger rate limit (exec/sec, 0 for max)");
        System.out.println("  --failure-rate <0.0-1.0>                   Target task failure rate (e.g. 0.1 for 10%)");
        System.out.println("  --timeout <Ns|Nm>                          Execution timeout (e.g. 180s or 5m)");
        System.out.println("  --cleanup                                  Clean up benchmark-created jobs/executions afterwards");
        System.out.println("  --gateway-url <url>                        API Gateway URL (default: http://localhost:8080)");
        System.out.println("  --kafka-bootstrap <servers>                Kafka bootstrap servers (default: localhost:9092)");
        System.out.println("  --redis-host <host>                        Redis host (default: localhost)");
        System.out.println("  --prometheus-url <url>                     Prometheus URL (default: http://localhost:9090)");
        System.out.println("  --api-key <key>                            X-API-Key for authentication");
        System.out.println("  --help                                     Display this help menu");
    }
}
