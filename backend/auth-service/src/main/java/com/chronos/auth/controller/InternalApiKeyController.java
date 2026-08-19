package com.chronos.auth.controller;

import com.chronos.auth.dto.ApiKeyValidationResult;
import com.chronos.auth.dto.ValidateApiKeyRequest;
import com.chronos.auth.service.ApiKeyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api-keys")
public class InternalApiKeyController {

    private final ApiKeyService apiKeyService;

    public InternalApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiKeyValidationResult> validateApiKey(@Valid @RequestBody ValidateApiKeyRequest request) {
        ApiKeyValidationResult result = apiKeyService.validateApiKey(request.getApiKey());
        return ResponseEntity.ok(result);
    }
}
