package com.chronos.worker.task;

import com.chronos.worker.event.ExecutionDispatchedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DemoReportTaskHandlerTest {

    private DemoReportTaskHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DemoReportTaskHandler();
    }

    @Test
    void testExecuteDemoReportTaskSuccessfully() {
        UUID executionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        ExecutionDispatchedEvent event = new ExecutionDispatchedEvent(
                executionId,
                jobId,
                orgId,
                1,
                "DEMO_REPORT",
                "Payload data",
                Instant.now()
        );

        String result = handler.execute(event);

        assertNotNull(result);
        assertTrue(result.contains("Demo report generated successfully"));
        assertTrue(result.contains(jobId.toString()));
        assertTrue(result.contains("attempt=1"));
    }

    @Test
    void testExecuteWithNullEventThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> handler.execute(null));
    }
}
