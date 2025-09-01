package com.common.context;

import java.util.Optional;
import java.util.UUID;

/**
 * Convenience wrapper around {@link ContextManager.Tenant} to work with
 * {@link java.util.UUID} tenant identifiers.
 */
public final class TenantContext {

    private TenantContext() {
        // utility class
    }

    /**
     * Set the current tenant identifier; pass {@code null} to clear.
     *
     * @param tenantId the tenant identifier
     */
    public static void set(UUID tenantId) {
        if (tenantId == null) {
            ContextManager.Tenant.clear();
        } else {
            ContextManager.Tenant.set(tenantId.toString());
        }
    }

    /**
     * Retrieve the current tenant identifier if present.
     *
     * @return optional tenant identifier
     */
    public static Optional<UUID> get() {
        return Optional.ofNullable(ContextManager.Tenant.get())
                .filter(id -> !id.isBlank())
                .map(UUID::fromString);
    }

    /**
     * Clear the tenant information from the current context.
     */
    public static void clear() {
        ContextManager.Tenant.clear();
    }
}
