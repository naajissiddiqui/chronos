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

        logger.info("Executing task: taskType=DEMO_REPORT for executionId={}, jobId={}, organizationId={}",
                event.getExecutionId(), event.getJobId(), event.getOrganizationId());

        // Perform safe deterministic demo operation
        String result = "Demo report generated successfully for jobId=" + event.getJobId() + " (attempt=" + event.getAttempt() + ")";
        logger.info("Completed DEMO_REPORT execution: executionId={}, result='{}'", event.getExecutionId(), result);

        return result;
    }
}
