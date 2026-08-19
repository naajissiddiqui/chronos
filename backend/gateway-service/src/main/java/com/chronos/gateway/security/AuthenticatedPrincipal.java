package com.chronos.gateway.security;

import java.util.UUID;

public class AuthenticatedPrincipal {

    private final UUID userId;
    private final UUID organizationId;
    private final String role;
    private final String authType; // "JWT" or "API_KEY"

    public AuthenticatedPrincipal(UUID userId, UUID organizationId, String role, String authType) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.role = role;
        this.authType = authType;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getRole() {
        return role;
    }

    public String getAuthType() {
        return authType;
    }
}
