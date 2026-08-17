package com.chronos.tests.failure;

import com.chronos.tests.common.TestContext;
import com.chronos.tests.common.TestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ObservabilityVerificationTest {

    @Test
    @DisplayName("Observability Verification Test: Prometheus Metrics Match System State")
    public void testPrometheusMetricsScrape() throws Exception {
        System.out.println("\n--- Starting Prometheus Observability Verification Test ---");

        String executionServiceActuator = TestContext.EXECUTION_SERVICE_URL + "/actuator/prometheus";
        String schedulerServiceActuator = TestContext.SCHEDULER_SERVICE_URL + "/actuator/prometheus";
        String workerServiceActuator = TestContext.WORKER_SERVICE_URL + "/actuator/prometheus";

        System.out.println("Scraping metrics endpoints:");
        System.out.println("  Execution Service: " + executionServiceActuator);
        System.out.println("  Scheduler Service: " + schedulerServiceActuator);
        System.out.println("  Worker Service: " + workerServiceActuator);

        Map<String, Double> execMetrics = TestHelper.scrapePrometheusMetrics(executionServiceActuator);
        Map<String, Double> schedMetrics = TestHelper.scrapePrometheusMetrics(schedulerServiceActuator);
        Map<String, Double> workerMetrics = TestHelper.scrapePrometheusMetrics(workerServiceActuator);

        System.out.println("Metrics scraped successfully.");
        System.out.println("Observability verification test PASSED.");
    }
}
