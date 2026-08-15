package com.chronos.execution.repository;

import com.chronos.execution.entity.Execution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, UUID> {

    List<Execution> findByOrganizationId(UUID organizationId);

    Optional<Execution> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Execution> findByJobIdAndOrganizationId(UUID jobId, UUID organizationId);

    boolean existsBySourceEventId(UUID sourceEventId);

    Optional<Execution> findBySourceEventId(UUID sourceEventId);

    List<Execution> findByStatusAndNextAttemptAtLessThanEqual(com.chronos.execution.entity.ExecutionStatus status, java.time.Instant now);
}
