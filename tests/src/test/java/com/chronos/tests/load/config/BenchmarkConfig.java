package com.chronos.tests.load.config;

import java.util.UUID;

public class BenchmarkConfig {

    public enum Scenario {
        SMALL(10, 10, 0.0),
        MEDIUM(100, 100, 0.0),
        LARGE(1000, 1000, 0.0),
        STRESS(500, 2000, 0.0),
        FAILURE(50, 50, 1.0);

        private final int defaultJobs;
        private final int defaultExecutions;
        private final double defaultFailureRate;

        Scenario(int defaultJobs, int defaultExecutions, double defaultFailureRate) {
            this.defaultJobs = defaultJobs;
            this.defaultExecutions = defaultExecutions;
            this.defaultFailureRate = defaultFailureRate;
        }

        public int getDefaultJobs() {
            return defaultJobs;
        }

        public int getDefaultExecutions() {
            return defaultExecutions;
        }

        public double getDefaultFailureRate() {
            return defaultFailureRate;
        }
    }

    public enum PipelineTriggerMode {
        SCHEDULER,    // True E2E: Job Service -> DB -> Scheduler Service -> Outbox -> Kafka -> Execution -> Worker
        KAFKA_DIRECT  // Direct Kafka injection: Benchmark -> Kafka job.triggered -> Execution -> Worker (for throughput micro-benchmarks)
    }

    private Scenario scenario = Scenario.SMALL;
    private PipelineTriggerMode triggerMode = PipelineTriggerMode.SCHEDULER;
    private int jobs = 10;
    private int executions = 10;
    private int concurrency = 10;
    private double failureRate = 0.0;
    private int timeoutSeconds = 180;
    private boolean cleanup = false;
    private int rateLimit = 0; // 0 = unthrottled

    private String runId;
    private UUID organizationId;

    private String gatewayUrl = "http://localhost:8080";
    private String authServiceUrl = "http://localhost:8081";
    private String jobServiceUrl = "http://localhost:8082";
    private String schedulerServiceUrl = "http://localhost:8083";
    private String executionServiceUrl = "http://localhost:8084";
    private String workerServiceUrl = "http://localhost:8085";

    private String kafkaBootstrapServers = "localhost:9092";
    private String redisHost = "localhost";
    private int redisPort = 6379;
    private String prometheusUrl = "http://localhost:9090";

    private String apiKey;
    private String jwtToken;
    private boolean useGateway = true;
    private long pollIntervalMs = 500;

    public BenchmarkConfig() {
        this.runId = "chronos-loadtest-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);
        this.organizationId = UUID.randomUUID();
    }

    public static BenchmarkConfig createDefault(Scenario scenario) {
        BenchmarkConfig config = new BenchmarkConfig();
        config.setScenario(scenario);
        config.setJobs(scenario.getDefaultJobs());
        config.setExecutions(scenario.getDefaultExecutions());
        config.setFailureRate(scenario.getDefaultFailureRate());
        return config;
    }

    // Getters and Setters
    public Scenario getScenario() {
        return scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public PipelineTriggerMode getTriggerMode() {
        return triggerMode;
    }

    public void setTriggerMode(PipelineTriggerMode triggerMode) {
        this.triggerMode = triggerMode;
    }

    public int getJobs() {
        return jobs;
    }

    public void setJobs(int jobs) {
        this.jobs = Math.max(1, jobs);
    }

    public int getExecutions() {
        return executions;
    }

    public void setExecutions(int executions) {
        this.executions = Math.max(1, executions);
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = Math.max(1, concurrency);
    }

    public double getFailureRate() {
        return failureRate;
    }

    public void setFailureRate(double failureRate) {
        this.failureRate = Math.max(0.0, Math.min(1.0, failureRate));
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = Math.max(5, timeoutSeconds);
    }

    public boolean isCleanup() {
        return cleanup;
    }

    public void setCleanup(boolean cleanup) {
        this.cleanup = cleanup;
    }

    public int getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(int rateLimit) {
        this.rateLimit = Math.max(0, rateLimit);
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public String getAuthServiceUrl() {
        return authServiceUrl;
    }

    public void setAuthServiceUrl(String authServiceUrl) {
        this.authServiceUrl = authServiceUrl;
    }

    public String getJobServiceUrl() {
        return jobServiceUrl;
    }

    public void setJobServiceUrl(String jobServiceUrl) {
        this.jobServiceUrl = jobServiceUrl;
    }

    public String getSchedulerServiceUrl() {
        return schedulerServiceUrl;
    }

    public void setSchedulerServiceUrl(String schedulerServiceUrl) {
        this.schedulerServiceUrl = schedulerServiceUrl;
    }

    public String getExecutionServiceUrl() {
        return executionServiceUrl;
    }

    public void setExecutionServiceUrl(String executionServiceUrl) {
        this.executionServiceUrl = executionServiceUrl;
    }

    public String getWorkerServiceUrl() {
        return workerServiceUrl;
    }

    public void setWorkerServiceUrl(String workerServiceUrl) {
        this.workerServiceUrl = workerServiceUrl;
    }

    public String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    public void setKafkaBootstrapServers(String kafkaBootstrapServers) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    public String getRedisHost() {
        return redisHost;
    }

    public void setRedisHost(String redisHost) {
        this.redisHost = redisHost;
    }

    public int getRedisPort() {
        return redisPort;
    }

    public void setRedisPort(int redisPort) {
        this.redisPort = redisPort;
    }

    public String getPrometheusUrl() {
        return prometheusUrl;
    }

    public void setPrometheusUrl(String prometheusUrl) {
        this.prometheusUrl = prometheusUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public boolean isUseGateway() {
        return useGateway;
    }

    public void setUseGateway(boolean useGateway) {
        this.useGateway = useGateway;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = Math.max(100, pollIntervalMs);
    }
}
