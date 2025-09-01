package com.lms.tenant.config;

import java.util.Optional;
import java.util.UUID;

/**
 * Holds the tenant identifier for the current request using {@link ThreadLocal} storage.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    /**
     * Set the tenant identifier for the current thread.
     *
     * @param tenantId tenant identifier
     */
    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Retrieve the tenant identifier for the current thread.
     *
     * @return optional tenant identifier
     */
    public static Optional<UUID> getTenantId() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    /**
     * Clear the tenant information from the current thread.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
