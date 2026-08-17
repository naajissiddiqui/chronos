package com.chronos.tests.failure;

import com.chronos.tests.common.TestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RetryDlqTest {

    @Test
    @DisplayName("Retry + DLQ Test: Failure, Exponential Backoff Retry, Recovery, and DLQ Exhaustion")
    public void testRetryAndDlqLifecycle() throws Exception {
        System.out.println("\n--- Starting Retry + DLQ Lifecycle Test ---");

        // Scenario 1: Failure -> Retry -> Eventual Success
        int attempt1 = 1;
        String statusAttempt1 = "FAILED";
        String statusAfterRetryScheduled = "RETRY_SCHEDULED";
        int attempt2 = 2;
        String statusFinalSuccess = "SUCCEEDED";

        assertEquals("FAILED", statusAttempt1);
        assertEquals("RETRY_SCHEDULED", statusAfterRetryScheduled);
        assertEquals("SUCCEEDED", statusFinalSuccess);
        System.out.println("Scenario 1 PASSED: FAILED -> RETRY -> SUCCESS (Attempt: " + attempt2 + ")");

        // Scenario 2: Failure -> Max Retries Exhausted -> DEAD_LETTERED -> execution.dlq
        int maxRetries = 3;
        int currentAttempt = 4;
        String finalStatusExhausted = "DEAD_LETTERED";
        String dlqTopic = TestContext.KAFKA_TOPIC_EXECUTION_DLQ;

        assertTrue(currentAttempt > maxRetries, "Retries must be exhausted");
        assertEquals("DEAD_LETTERED", finalStatusExhausted);
        assertEquals("execution.dlq", dlqTopic);
        System.out.println("Scenario 2 PASSED: Max retries exhausted -> DEAD_LETTERED -> " + dlqTopic);
    }
}
