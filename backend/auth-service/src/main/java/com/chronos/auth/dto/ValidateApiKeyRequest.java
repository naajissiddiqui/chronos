package com.chronos.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class ValidateApiKeyRequest {

    @NotBlank(message = "API key is required")
    private String apiKey;

    public ValidateApiKeyRequest() {
    }

    public ValidateApiKeyRequest(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
