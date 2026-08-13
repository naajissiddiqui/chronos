package com.chronos.job.repository;

import com.chronos.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findAllByOrganizationId(UUID organizationId);

    Optional<Job> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long deleteByIdAndOrganizationId(UUID id, UUID organizationId);
}
