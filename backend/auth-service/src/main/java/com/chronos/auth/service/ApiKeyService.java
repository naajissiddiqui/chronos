package com.chronos.auth.service;

import com.chronos.auth.dto.*;

import java.util.List;
import java.util.UUID;

public interface ApiKeyService {

    CreateApiKeyResponse createApiKey(UUID organizationId, UUID userId, CreateApiKeyRequest request);

    List<ApiKeyResponse> listApiKeys(UUID organizationId);

    void revokeApiKey(UUID keyId, UUID organizationId);

    ApiKeyValidationResult validateApiKey(String rawKey);
}
