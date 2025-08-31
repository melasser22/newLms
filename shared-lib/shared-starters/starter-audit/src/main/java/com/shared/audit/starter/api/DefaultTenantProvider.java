package com.shared.audit.starter.api;

import com.common.context.ContextManager;

/**
 * Default {@link TenantProvider} implementation that resolves the tenant
 * identifier from the shared {@link ContextManager}. The context is typically
 * populated by HTTP filters or other infrastructure components when a request
 * enters the application.
 */
public class DefaultTenantProvider implements TenantProvider {

    /**
     * Return the current tenant identifier or {@code null} if none is set.
     */
    @Override
    public String getTenantId() {
        return ContextManager.Tenant.get();
    }
}