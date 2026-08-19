package com.chronos.auth.dto;

import java.util.UUID;

public class ApiKeyValidationResult {

    private boolean valid;
    private UUID userId;
    private UUID organizationId;
    private String role;
    private UUID keyId;
    private String errorReason;

    public ApiKeyValidationResult() {
    }

    public static ApiKeyValidationResult success(UUID userId, UUID organizationId, String role, UUID keyId) {
        ApiKeyValidationResult result = new ApiKeyValidationResult();
        result.setValid(true);
        result.setUserId(userId);
        result.setOrganizationId(organizationId);
        result.setRole(role);
        result.setKeyId(keyId);
        return result;
    }

    public static ApiKeyValidationResult invalid(String errorReason) {
        ApiKeyValidationResult result = new ApiKeyValidationResult();
        result.setValid(false);
        result.setErrorReason(errorReason);
        return result;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public UUID getKeyId() {
        return keyId;
    }

    public void setKeyId(UUID keyId) {
        this.keyId = keyId;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }
}
