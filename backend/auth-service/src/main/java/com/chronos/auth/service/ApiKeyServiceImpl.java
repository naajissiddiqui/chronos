package com.chronos.auth.service;

import com.chronos.auth.dto.*;
import com.chronos.auth.entity.ApiKey;
import com.chronos.auth.entity.User;
import com.chronos.auth.exception.BadRequestException;
import com.chronos.auth.exception.ResourceNotFoundException;
import com.chronos.auth.repository.ApiKeyRepository;
import com.chronos.auth.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApiKeyServiceImpl implements ApiKeyService {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyServiceImpl.class);
    private static final String KEY_PREFIX_HEADER = "chron_";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final MeterRegistry meterRegistry;

    @Autowired
    public ApiKeyServiceImpl(ApiKeyRepository apiKeyRepository,
                              UserRepository userRepository,
                              @Autowired(required = false) MeterRegistry meterRegistry) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional
    public CreateApiKeyResponse createApiKey(UUID organizationId, UUID userId, CreateApiKeyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!user.getOrganization().getId().equals(organizationId)) {
            throw new BadRequestException("User does not belong to organization ID: " + organizationId);
        }

        // Generate 8-char random prefix
        String prefix = generateRandomString(8);
        // Generate 32-byte secret -> base64url
        byte[] secretBytes = new byte[32];
        SECURE_RANDOM.nextBytes(secretBytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

        String rawKey = KEY_PREFIX_HEADER + prefix + "_" + secret;
        String keyHash = hashKey(rawKey);

        ApiKey apiKey = new ApiKey(organizationId, userId, request.getName(), prefix, keyHash, request.getExpiresAt());
        ApiKey saved = apiKeyRepository.save(apiKey);

        incrementCounter("api_keys_created_total");
        logger.info("Created API Key id={}, name={}, prefix={} for orgId={}", saved.getId(), saved.getName(), prefix, organizationId);

        return CreateApiKeyResponse.fromEntity(saved, rawKey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listApiKeys(UUID organizationId) {
        return apiKeyRepository.findByOrganizationId(organizationId)
                .stream()
                .map(ApiKeyResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revokeApiKey(UUID keyId, UUID organizationId) {
        ApiKey apiKey = apiKeyRepository.findByIdAndOrganizationId(keyId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("API Key not found with ID: " + keyId));

        if (!apiKey.isActive()) {
            return;
        }

        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);

        incrementCounter("api_keys_revoked_total");
        logger.info("Revoked API Key id={} for orgId={}", keyId, organizationId);
    }

    @Override
    @Transactional
    public ApiKeyValidationResult validateApiKey(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(KEY_PREFIX_HEADER)) {
            incrementCounter("api_key_auth_failure_total");
            return ApiKeyValidationResult.invalid("Invalid key format");
        }

        String keyHash = hashKey(rawKey);
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyHash(keyHash);

        if (apiKeyOpt.isEmpty()) {
            incrementCounter("api_key_auth_failure_total");
            return ApiKeyValidationResult.invalid("API key not found");
        }

        ApiKey apiKey = apiKeyOpt.get();

        if (!apiKey.isActive()) {
            incrementCounter("api_key_auth_failure_total");
            return ApiKeyValidationResult.invalid("API key is revoked");
        }

        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(Instant.now())) {
            incrementCounter("api_key_auth_failure_total");
            return ApiKeyValidationResult.invalid("API key is expired");
        }

        User user = userRepository.findById(apiKey.getUserId()).orElse(null);
        if (user == null || !user.isEnabled()) {
            incrementCounter("api_key_auth_failure_total");
            return ApiKeyValidationResult.invalid("User account disabled or not found");
        }

        // Update last used timestamp
        apiKey.setLastUsedAt(Instant.now());
        apiKeyRepository.save(apiKey);

        incrementCounter("api_key_auth_success_total");
        return ApiKeyValidationResult.success(
                user.getId(),
                apiKey.getOrganizationId(),
                user.getRole().name(),
                apiKey.getId()
        );
    }

    private String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private void incrementCounter(String metricName) {
        if (meterRegistry != null) {
            Counter.builder(metricName).register(meterRegistry).increment();
        }
    }
}
