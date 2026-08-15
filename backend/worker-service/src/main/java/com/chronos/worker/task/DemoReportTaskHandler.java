package com.chronos.worker.task;

import com.chronos.worker.event.ExecutionDispatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DemoReportTaskHandler {

    private static final Logger logger = LoggerFactory.getLogger(DemoReportTaskHandler.class);

    public String execute(ExecutionDispatchedEvent event) {
        if (event == null || event.getExecutionId() == null) {
            throw new IllegalArgumentException("ExecutionDispatchedEvent cannot be null or missing executionId");
        }

        logger.info("Executing task: taskType={} for executionId={}, jobId={}, organizationId={}, attempt={}",
                event.getTaskType(), event.getExecutionId(), event.getJobId(), event.getOrganizationId(), event.getAttempt());

        if ("DEMO_REPORT_FAIL".equalsIgnoreCase(event.getTaskType())) {
            logger.warn("Controlled failure triggered for DEMO_REPORT_FAIL: executionId={}, attempt={}",
                    event.getExecutionId(), event.getAttempt());
            throw new RuntimeException("Controlled failure for DEMO_REPORT_FAIL (attempt=" + event.getAttempt() + ")");
        }

        // Perform safe deterministic demo operation
        String result = "Demo report generated successfully for jobId=" + event.getJobId() + " (attempt=" + event.getAttempt() + ")";
        logger.info("Completed DEMO_REPORT execution: executionId={}, result='{}'", event.getExecutionId(), result);

        return result;
    }
}
