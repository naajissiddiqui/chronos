package com.chronos.auth.repository;

import com.chronos.auth.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    List<ApiKey> findByKeyPrefix(String keyPrefix);

    List<ApiKey> findByOrganizationId(UUID organizationId);

    Optional<ApiKey> findByIdAndOrganizationId(UUID id, UUID organizationId);
    
    Optional<ApiKey> findByKeyHash(String keyHash);
}
