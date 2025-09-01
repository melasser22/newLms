package com.lms.tenant.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * A ThreadLocal holder for the current tenant's ID. This allows easy access
 * to the tenant context throughout the application without passing it as a
 * method parameter.
 */
public final class TenantContext {

    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);
    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Private constructor to prevent instantiation
    }

    /**
     * Sets the tenant ID for the current thread.
     * @param tenantId The UUID of the current tenant.
     */
    public static void setTenantId(UUID tenantId) {
        if (tenantId == null) {
            log.warn("Tenant ID is being set to null.");
        }
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Retrieves the tenant ID for the current thread.
     * @return An Optional containing the tenant ID, or empty if not set.
     */
    public static Optional<UUID> getTenantId() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    /**
     * Clears the tenant context for the current thread. This should be called
     * in a finally block to prevent memory leaks in application server environments.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
