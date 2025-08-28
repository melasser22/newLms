package com.common.context;

import java.util.function.Supplier;

/**
 * Default ContextCarrier implementation using thread-local storage.
 * It provides scoped setters for tenant ID, correlation ID, request ID, and user ID,
 * along with utilities to temporarily override these values and automatically restore previous values.
 */
public class ThreadLocalContextCarrier implements ContextCarrier {

    private final ThreadLocal<String> tenantId  = new ThreadLocal<>();
    private final ThreadLocal<String> correlationId = new ThreadLocal<>();
    private final ThreadLocal<String> requestId = new ThreadLocal<>();
    private final ThreadLocal<String> userId    = new ThreadLocal<>();

    // ==================== Tenant ====================

    @Override
    public void setTenantId(String id) {
        if (id == null || id.isBlank()) {
            tenantId.remove();
        } else {
            tenantId.set(id);
        }
    }

    @Override
    public String getTenantId() {
        return tenantId.get();
    }

    @Override
    public void clearTenantId() {
        tenantId.remove();
    }

    @Override
    public AutoCloseable openTenantScope(String id) {
        String previous = tenantId.get();
        setTenantId(id);
        return () -> {
            if (previous == null || previous.isBlank()) {
                tenantId.remove();
            } else {
                tenantId.set(previous);
            }
        };
    }

    @Override
    public <T> T callWithTenant(String id, Supplier<T> supplier) {
        try (AutoCloseable scope = openTenantScope(id)) {
            return supplier.get();
        } catch (Exception e) {
            // AutoCloseable#close can't throw checked exceptions, so this catch is here
            // in case the supplier throws. We propagate as runtime exceptions.
            throw new RuntimeException(e);
        }
    }

    // ==================== Correlation ID ====================

    @Override
    public void setCorrelationId(String id) {
        if (id == null || id.isBlank()) {
            correlationId.remove();
        } else {
            correlationId.set(id);
        }
    }

    @Override
    public String getCorrelationId() {
        return correlationId.get();
    }

    @Override
    public void clearCorrelationId() {
        correlationId.remove();
    }

    @Override
    public AutoCloseable openCorrelationScope(String id) {
        String previous = correlationId.get();
        setCorrelationId(id);
        return () -> {
            if (previous == null) {
                correlationId.remove();
            } else {
                correlationId.set(previous);
            }
        };
    }

    // ==================== Request ID ====================

    @Override
    public void setRequestId(String id) {
        if (id == null || id.isBlank()) {
            requestId.remove();
        } else {
            requestId.set(id);
        }
    }

    @Override
    public String getRequestId() {
        return requestId.get();
    }

    @Override
    public void clearRequestId() {
        requestId.remove();
    }

    @Override
    public AutoCloseable openRequestScope(String id) {
        String previous = requestId.get();
        setRequestId(id);
        return () -> {
            if (previous == null) {
                requestId.remove();
            } else {
                requestId.set(previous);
            }
        };
    }

    // ==================== User ID ====================

    @Override
    public void setUserId(String id) {
        if (id == null || id.isBlank()) {
            userId.remove();
        } else {
            userId.set(id);
        }
    }

    @Override
    public String getUserId() {
        return userId.get();
    }

    @Override
    public void clearUserId() {
        userId.remove();
    }

    @Override
    public AutoCloseable openUserScope(String id) {
        String previous = userId.get();
        setUserId(id);
        return () -> {
            if (previous == null) {
                userId.remove();
            } else {
                userId.set(previous);
            }
        };
    }

    // ==================== General ====================

    @Override
    public void clearAll() {
        tenantId.remove();
        correlationId.remove();
        requestId.remove();
        userId.remove();
    }
}
