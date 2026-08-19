package com.chronos.auth.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class CreateApiKeyRequest {

    @NotBlank(message = "API key name is required")
    private String name;

    private Instant expiresAt;

    public CreateApiKeyRequest() {
    }

    public CreateApiKeyRequest(String name, Instant expiresAt) {
        this.name = name;
        this.expiresAt = expiresAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
