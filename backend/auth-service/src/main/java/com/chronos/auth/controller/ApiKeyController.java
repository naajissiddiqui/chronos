package com.chronos.auth.controller;

import com.chronos.auth.dto.ApiKeyResponse;
import com.chronos.auth.dto.CreateApiKeyRequest;
import com.chronos.auth.dto.CreateApiKeyResponse;
import com.chronos.auth.security.CustomUserDetails;
import com.chronos.auth.service.ApiKeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    public ResponseEntity<CreateApiKeyResponse> createApiKey(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateApiKeyRequest request) {
        CreateApiKeyResponse response = apiKeyService.createApiKey(
                userDetails.getOrganizationId(),
                userDetails.getId(),
                request
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> listApiKeys(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ApiKeyResponse> responses = apiKeyService.listApiKeys(userDetails.getOrganizationId());
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeApiKey(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") UUID id) {
        apiKeyService.revokeApiKey(id, userDetails.getOrganizationId());
        return ResponseEntity.noContent().build();
    }
}
