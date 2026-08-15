package com.chronos.execution.service;

import com.chronos.execution.entity.Execution;
import com.chronos.execution.entity.ExecutionStatus;
import com.chronos.execution.event.ExecutionDispatchedEvent;
import com.chronos.execution.kafka.KafkaExecutionDispatchProducer;
import com.chronos.execution.repository.ExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class RetrySchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(RetrySchedulerService.class);

    private final ExecutionRepository executionRepository;
    private final KafkaExecutionDispatchProducer kafkaExecutionDispatchProducer;

    public RetrySchedulerService(
            ExecutionRepository executionRepository,
            KafkaExecutionDispatchProducer kafkaExecutionDispatchProducer) {
        this.executionRepository = executionRepository;
        this.kafkaExecutionDispatchProducer = kafkaExecutionDispatchProducer;
    }

    @Scheduled(fixedDelayString = "${retry.poller.delay:1000}")
    public void pollAndDispatchDueRetries() {
        Instant now = Instant.now();
        List<Execution> dueRetries = executionRepository.findByStatusAndNextAttemptAtLessThanEqual(
                ExecutionStatus.RETRY_SCHEDULED, now
        );

        if (dueRetries.isEmpty()) {
            return;
        }

        for (Execution execution : dueRetries) {
            dispatchSingleRetry(execution);
        }
    }

    @Transactional
    public boolean dispatchSingleRetry(Execution execution) {
        try {
            if (execution == null || execution.getStatus() != ExecutionStatus.RETRY_SCHEDULED) {
                return false;
            }

            execution.setStatus(ExecutionStatus.PENDING);
            execution.setNextAttemptAt(null);
            Execution saved = executionRepository.saveAndFlush(execution);

            logger.info("Retry dispatched: executionId={}, attempt={}", saved.getId(), saved.getAttempt());

            // Construct payload - default to DEMO_REPORT or DEMO_REPORT_FAIL if error message indicates controlled fail
            String taskType = "DEMO_REPORT";
            if (saved.getErrorMessage() != null && saved.getErrorMessage().contains("DEMO_REPORT_FAIL")) {
                taskType = "DEMO_REPORT_FAIL";
            }

            ExecutionDispatchedEvent dispatchEvent = new ExecutionDispatchedEvent(
                    saved.getId(),
                    saved.getJobId(),
                    saved.getOrganizationId(),
                    saved.getAttempt(),
                    taskType,
                    "Retry attempt " + saved.getAttempt() + " for jobId=" + saved.getJobId(),
                    Instant.now()
            );

            kafkaExecutionDispatchProducer.sendExecutionDispatched(dispatchEvent);
            return true;
        } catch (Exception e) {
            logger.error("Error dispatching retry for executionId={}: {}", execution.getId(), e.getMessage(), e);
            return false;
        }
    }
}
