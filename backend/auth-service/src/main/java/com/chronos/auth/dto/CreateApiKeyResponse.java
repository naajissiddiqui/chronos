package com.chronos.auth.dto;

import com.chronos.auth.entity.ApiKey;
import java.time.Instant;
import java.util.UUID;

public class CreateApiKeyResponse {

    private UUID id;
    private UUID organizationId;
    private UUID userId;
    private String name;
    private String keyPrefix;
    private String rawKey;
    private boolean active;
    private Instant createdAt;
    private Instant expiresAt;

    public CreateApiKeyResponse() {
    }

    public static CreateApiKeyResponse fromEntity(ApiKey apiKey, String rawKey) {
        CreateApiKeyResponse dto = new CreateApiKeyResponse();
        dto.setId(apiKey.getId());
        dto.setOrganizationId(apiKey.getOrganizationId());
        dto.setUserId(apiKey.getUserId());
        dto.setName(apiKey.getName());
        dto.setKeyPrefix(apiKey.getKeyPrefix());
        dto.setRawKey(rawKey);
        dto.setActive(apiKey.isActive());
        dto.setCreatedAt(apiKey.getCreatedAt());
        dto.setExpiresAt(apiKey.getExpiresAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getRawKey() {
        return rawKey;
    }

    public void setRawKey(String rawKey) {
        this.rawKey = rawKey;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
