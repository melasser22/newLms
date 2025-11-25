package com.ejada.common.context;

import java.util.function.Supplier;

/**
 * Centralized context holder for tenant, correlation ID, request ID, and user ID
 * using thread-local storage.
 */
public final class ContextManager {

    private static final ThreadLocal<String> tenantId      = new ThreadLocal<>();
    private static final ThreadLocal<String> correlationId = new ThreadLocal<>();
    private static final ThreadLocal<String> requestId     = new ThreadLocal<>();
    private static final ThreadLocal<String> userId        = new ThreadLocal<>();

    private ContextManager() {
        // utility class
    }

    // ==================== Tenant ====================

    public static final class Tenant {

        private Tenant() {
            // nested type
        }

        /**
         * Set the current tenant identifier; pass null/blank to clear.
         */
        public static void set(String tenantId) {
            setOrClear(ContextManager.tenantId, tenantId);
        }

        /**
         * Get the current tenant identifier or null if none is set.
         */
        public static String get() {
            return tenantId.get();
        }

        /**
         * Whether a non‑blank tenant identifier is present.
         */
        public static boolean isPresent() {
            String v = get();
            return v != null && !v.isBlank();
        }

        /**
         * Clear the tenant identifier from the current context.
         */
        public static void clear() {
            tenantId.remove();
        }

        /**
         * Set the tenant for the duration of the scope and restore the previous
         * value when closed.
         */
        public static Scope openScope(String tenantId) {
            String previous = ContextManager.tenantId.get();
            set(tenantId);
            return new Scope(previous);
        }

        /**
         * Run a block with the given tenant identifier and restore afterwards.
         */
        public static void runWith(String tenantId, Runnable runnable) {
            try (Scope ignore = openScope(tenantId)) {
                runnable.run();
            }
        }

        /**
         * Call a supplier with the given tenant identifier and restore afterwards.
         */
        public static <T> T callWith(String tenantId, Supplier<T> supplier) {
            try (Scope ignore = openScope(tenantId)) {
                return supplier.get();
            }
        }

        /**
         * Scope wrapper to temporarily override tenant context.
         */
        public static final class Scope implements AutoCloseable {
            private final String previousValue;
            private boolean closed;

            private Scope(String previousValue) {
                this.previousValue = previousValue;
            }

            @Override
            public void close() {
                if (closed) {
                    return;
                }
                setOrClear(ContextManager.tenantId, previousValue);
                closed = true;
            }
        }
    }

    // ==================== Correlation ID ====================

    public static void setCorrelationId(String id) {
        setOrClear(correlationId, id);
    }

    public static String getCorrelationId() {
        return correlationId.get();
    }

    public static void clearCorrelationId() {
        correlationId.remove();
    }

    // ==================== Request ID ====================

    public static void setRequestId(String id) {
        setOrClear(requestId, id);
    }

    public static String getRequestId() {
        return requestId.get();
    }

    public static void clearRequestId() {
        requestId.remove();
    }

    // ==================== User ID ====================

    public static void setUserId(String id) {
        setOrClear(userId, id);
    }

    public static String getUserId() {
        return userId.get();
    }

    public static void clearUserId() {
        userId.remove();
    }

    /**
     * Clear all header-related contexts: correlation, request, tenant and user.
     */
    public static void clearHeaders() {
        correlationId.remove();
        requestId.remove();
        tenantId.remove();
        userId.remove();
    }

    private static void setOrClear(ThreadLocal<String> holder, String value) {
        if (value == null || value.isBlank()) {
            holder.remove();
        } else {
            holder.set(value);
        }
    }
}
