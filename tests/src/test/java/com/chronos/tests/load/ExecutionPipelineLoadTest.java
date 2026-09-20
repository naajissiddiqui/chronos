package com.chronos.tests.load;

import com.chronos.tests.load.config.BenchmarkConfig;
import com.chronos.tests.load.metrics.PipelineMetricsCollector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionPipelineLoadTest {

    @Test
    @DisplayName("Load Scenario - Small (10 Jobs, 10 Executions)")
    public void testLoad10Jobs() throws Exception {
        BenchmarkConfig config = BenchmarkConfig.createDefault(BenchmarkConfig.Scenario.SMALL);
        config.setTimeoutSeconds(30);

        PipelineMetricsCollector.FinalBenchmarkMetrics metrics = ChronosBenchmarkRunner.run(config);

        assertNotNull(metrics);
        assertEquals(10, metrics.jobsRequested);
        assertEquals(10, metrics.executionsRequested);
        assertTrue(metrics.executionsCompleted >= 0);
        assertTrue(metrics.throughputExecPerSec >= 0.0);
    }

    @Test
    @DisplayName("Load Scenario - Medium (50 Jobs, 50 Executions)")
    public void testLoad50Jobs() throws Exception {
        BenchmarkConfig config = new BenchmarkConfig();
        config.setScenario(BenchmarkConfig.Scenario.MEDIUM);
        config.setJobs(50);
        config.setExecutions(50);
        config.setTimeoutSeconds(45);

        PipelineMetricsCollector.FinalBenchmarkMetrics metrics = ChronosBenchmarkRunner.run(config);

        assertNotNull(metrics);
        assertEquals(50, metrics.jobsRequested);
        assertEquals(50, metrics.executionsRequested);
        assertTrue(metrics.executionsCompleted >= 0);
        assertTrue(metrics.throughputExecPerSec >= 0.0);
    }

    @Test
    @DisplayName("Load Scenario - 100 Jobs Benchmark")
    public void testLoad100Jobs() throws Exception {
        BenchmarkConfig config = BenchmarkConfig.createDefault(BenchmarkConfig.Scenario.MEDIUM);
        config.setTimeoutSeconds(60);

        PipelineMetricsCollector.FinalBenchmarkMetrics metrics = ChronosBenchmarkRunner.run(config);

        assertNotNull(metrics);
        assertEquals(100, metrics.jobsRequested);
        assertEquals(100, metrics.executionsRequested);
        assertTrue(metrics.executionsCompleted >= 0);
    }

    @Test
    @DisplayName("Failure Load Scenario - 20 Jobs with 50% Failure Rate")
    public void testFailureLoadScenario() throws Exception {
        BenchmarkConfig config = new BenchmarkConfig();
        config.setScenario(BenchmarkConfig.Scenario.FAILURE);
        config.setJobs(20);
        config.setExecutions(20);
        config.setFailureRate(0.5);
        config.setTimeoutSeconds(30);

        PipelineMetricsCollector.FinalBenchmarkMetrics metrics = ChronosBenchmarkRunner.run(config);

        assertNotNull(metrics);
        assertEquals(20, metrics.jobsRequested);
        assertEquals(20, metrics.executionsRequested);
        assertTrue(metrics.executionsCompleted + metrics.executionsFailed > 0);
    }
}
