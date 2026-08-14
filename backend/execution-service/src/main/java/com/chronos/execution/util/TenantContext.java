package com.chronos.execution.util;

import java.util.UUID;

public class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_ORGANIZATION_ID = new ThreadLocal<>();

    public static void setOrganizationId(UUID organizationId) {
        CURRENT_ORGANIZATION_ID.set(organizationId);
    }

    public static UUID getOrganizationId() {
        return CURRENT_ORGANIZATION_ID.get();
    }

    public static void clear() {
        CURRENT_ORGANIZATION_ID.remove();
    }
}
