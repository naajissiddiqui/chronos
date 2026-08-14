package com.chronos.execution.service;

import com.chronos.execution.dto.ExecutionResponse;
import com.chronos.execution.entity.Execution;
import com.chronos.execution.entity.ExecutionStatus;
import com.chronos.execution.event.JobTriggeredEvent;
import com.chronos.execution.exception.ResourceNotFoundException;
import com.chronos.execution.repository.ExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);

    private final ExecutionRepository executionRepository;

    public ExecutionService(ExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    @Transactional
    public Optional<Execution> createExecutionFromEvent(JobTriggeredEvent event) {
        if (event == null || event.getEventId() == null || event.getJobId() == null || event.getOrganizationId() == null) {
            logger.error("Received invalid or malformed JobTriggeredEvent: {}", event);
            return Optional.empty();
        }

        // Idempotency check 1: Application-level check
        Optional<Execution> existing = executionRepository.findBySourceEventId(event.getEventId());
        if (existing.isPresent()) {
            logger.info("Duplicate event ignored: eventId={}, existingExecutionId={}", event.getEventId(), existing.get().getId());
            return existing;
        }

        Instant scheduledAt = event.getScheduledAt() != null ? event.getScheduledAt() : Instant.now();
        Execution execution = new Execution(
                event.getJobId(),
                event.getOrganizationId(),
                event.getEventId(),
                ExecutionStatus.PENDING,
                1,
                scheduledAt
        );

        try {
            Execution saved = executionRepository.saveAndFlush(execution);
            logger.info("Execution created: executionId={}, jobId={}, organizationId={}, status=PENDING",
                    saved.getId(), saved.getJobId(), saved.getOrganizationId());
            return Optional.of(saved);
        } catch (DataIntegrityViolationException e) {
            // Idempotency check 2: Database uniqueness constraint fallback for concurrent duplicates
            logger.info("Duplicate event ignored via database constraint: eventId={}", event.getEventId());
            return executionRepository.findBySourceEventId(event.getEventId());
        }
    }

    @Transactional(readOnly = true)
    public List<ExecutionResponse> getExecutionsForOrganization(UUID organizationId) {
        return executionRepository.findByOrganizationId(organizationId)
                .stream()
                .map(ExecutionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExecutionResponse getExecutionByIdAndOrganization(UUID id, UUID organizationId) {
        return executionRepository.findByIdAndOrganizationId(id, organizationId)
                .map(ExecutionResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<ExecutionResponse> getExecutionsByJobIdAndOrganization(UUID jobId, UUID organizationId) {
        return executionRepository.findByJobIdAndOrganizationId(jobId, organizationId)
                .stream()
                .map(ExecutionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
