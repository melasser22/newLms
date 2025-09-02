package com.ejada.common.context;

import java.util.function.Supplier;

/**
 * Abstraction for storing and retrieving context (tenant, headers).
 */
public interface ContextCarrier {
    // Tenant
    void setTenantId(String tenantId);
    String getTenantId();
    void clearTenantId();
    AutoCloseable openTenantScope(String tenantId);
    <T> T callWithTenant(String tenantId, Supplier<T> supplier);

    // Correlation ID
    void setCorrelationId(String correlationId);
    String getCorrelationId();
    void clearCorrelationId();
    AutoCloseable openCorrelationScope(String id);

    // Request ID
    void setRequestId(String requestId);
    String getRequestId();
    void clearRequestId();
    AutoCloseable openRequestScope(String id);

    // User ID
    void setUserId(String userId);
    String getUserId();
    void clearUserId();
    AutoCloseable openUserScope(String id);

    void clearAll();
}
