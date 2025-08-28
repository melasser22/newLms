package com.shared.starter_core.context;

import com.common.context.ContextManager;

/**
 * Backwards-compatibility holder for tenant context.
 *
 * <p>Delegates to the unified {@link ContextManager.Tenant} implementation.
 * This class is retained to preserve existing API surface, but callers are
 * encouraged to migrate to {@link ContextManager} directly.</p>
 *
 * @deprecated Use {@link ContextManager.Tenant} for tenant context management instead.
 */
@Deprecated
public class TenantContextHolder {

    private TenantContextHolder() {
        // utility class
    }

    /**
     * Set the current tenant identifier on the context.
     *
     * @param tenantId the tenant identifier
     */
    public static void setTenantId(String tenantId) {
        ContextManager.Tenant.set(tenantId);
    }

    /**
     * Get the current tenant identifier from the context.
     *
     * @return current tenant id or {@code null} if none set
     */
    public static String getTenantId() {
        return ContextManager.Tenant.get();
    }

    /**
     * Clear the tenant identifier from the context.
     */
    public static void clear() {
        ContextManager.Tenant.clear();
    }
}
