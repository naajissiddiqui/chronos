package com.chronos.worker.kafka;

import com.chronos.worker.event.ExecutionCompletedEvent;
import com.chronos.worker.event.ExecutionDispatchedEvent;
import com.chronos.worker.event.ExecutionFailedEvent;
import com.chronos.worker.task.DemoReportTaskHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KafkaExecutionDispatchConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaExecutionDispatchConsumer.class);

    private final String workerId;
    private final DemoReportTaskHandler demoReportTaskHandler;
    private final KafkaWorkerResultProducer resultProducer;
    private final Set<String> processedExecutionAttempts = ConcurrentHashMap.newKeySet();

    public KafkaExecutionDispatchConsumer(
            @Value("${worker.id:worker-local-1}") String workerId,
            DemoReportTaskHandler demoReportTaskHandler,
            KafkaWorkerResultProducer resultProducer) {
        this.workerId = workerId;
        this.demoReportTaskHandler = demoReportTaskHandler;
        this.resultProducer = resultProducer;
    }

    @KafkaListener(
            topics = "${kafka.topics.execution-dispatch:execution.dispatch}",
            groupId = "${spring.kafka.consumer.group-id:chronos-worker}"
    )
    public void consume(ExecutionDispatchedEvent event) {
        if (event == null || event.getExecutionId() == null) {
            logger.warn("Worker '{}' received null or invalid ExecutionDispatchedEvent", workerId);
            return;
        }

        int attempt = event.getAttempt() != null ? event.getAttempt() : 1;
        String idempotencyKey = event.getExecutionId() + ":" + attempt;

        logger.info("Worker {} received execution: executionId={}, jobId={}, organizationId={}, attempt={}, taskType={}",
                workerId, event.getExecutionId(), event.getJobId(), event.getOrganizationId(), attempt, event.getTaskType());

        // Idempotency check: prevent duplicate processing of the same execution attempt on local worker instance
        if (!processedExecutionAttempts.add(idempotencyKey)) {
            logger.info("Worker {} skipping duplicate execution attempt event: key={}", workerId, idempotencyKey);
            return;
        }

        try {
            if ("DEMO_REPORT".equalsIgnoreCase(event.getTaskType()) || "DEMO_REPORT_FAIL".equalsIgnoreCase(event.getTaskType())) {
                String result = demoReportTaskHandler.execute(event);

                ExecutionCompletedEvent completedEvent = new ExecutionCompletedEvent(
                        event.getExecutionId(),
                        event.getJobId(),
                        event.getOrganizationId(),
                        workerId,
                        attempt,
                        Instant.now(),
                        result
                );
                resultProducer.sendExecutionCompleted(completedEvent);
                logger.info("Execution completed: executionId={}, workerId={}", event.getExecutionId(), workerId);
            } else {
                String errorMsg = "Unsupported task type: " + event.getTaskType();
                logger.error("Execution failed: executionId={}, workerId={}, error={}", event.getExecutionId(), workerId, errorMsg);

                ExecutionFailedEvent failedEvent = new ExecutionFailedEvent(
                        event.getExecutionId(),
                        event.getJobId(),
                        event.getOrganizationId(),
                        workerId,
                        attempt,
                        Instant.now(),
                        errorMsg
                );
                resultProducer.sendExecutionFailed(failedEvent);
            }
        } catch (Exception e) {
            logger.error("Execution failed due to exception: executionId={}, workerId={}, error={}",
                    event.getExecutionId(), workerId, e.getMessage(), e);

            ExecutionFailedEvent failedEvent = new ExecutionFailedEvent(
                    event.getExecutionId(),
                    event.getJobId(),
                    event.getOrganizationId(),
                    workerId,
                    attempt,
                    Instant.now(),
                    "Execution error: " + e.getMessage()
            );
            resultProducer.sendExecutionFailed(failedEvent);
        }
    }

    public String getWorkerId() {
        return workerId;
    }

    public Set<String> getProcessedExecutionAttempts() {
        return processedExecutionAttempts;
    }
}
