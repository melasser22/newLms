package com.common.context;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Facade over a pluggable {@link ContextCarrier}.  Provides static methods for
 * getting/setting tenant, correlation ID, request ID and user ID contexts.
 * Initially backed by a {@link ThreadLocalContextCarrier}, but can be replaced
 * via {@link #setContextCarrier(ContextCarrier)} for testing or alternative
 * context propagation mechanisms.
 */
public final class ContextManager {

    /**
     * The current context carrier.  Defaults to a thread‑local implementation.
     */
    private static ContextCarrier CARRIER = new ThreadLocalContextCarrier();

    private ContextManager() {
        // utility class
    }

    /**
     * Replace the current ContextCarrier.  Useful for tests or alternative carriers.
     *
     * @param carrier new context carrier (must not be null)
     */
    public static void setContextCarrier(ContextCarrier carrier) {
        CARRIER = Objects.requireNonNull(carrier, "carrier must not be null");
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
            CARRIER.setTenantId(tenantId);
        }

        /**
         * Get the current tenant identifier or null if none is set.
         */
        public static String get() {
            return CARRIER.getTenantId();
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
            CARRIER.clearTenantId();
        }

        /**
         * Set the tenant for the duration of the scope and restore the previous
         * value when closed.
         */
        public static Scope openScope(String tenantId) {
            AutoCloseable ac = CARRIER.openTenantScope(tenantId);
            return new Scope(ac);
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
         * Scope wrapper over ContextCarrier's AutoCloseable for tenant context.
         */
        public static final class Scope implements AutoCloseable {
            private final AutoCloseable inner;
            private boolean closed;

            private Scope(AutoCloseable inner) {
                this.inner = inner;
            }

            @Override
            public void close() {
                if (closed) {
                    return;
                }
                try {
                    inner.close();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to close context", e);
                }
                closed = true;
            }
        }
    }

    // ==================== Correlation ID ====================

    public static void setCorrelationId(String id) {
        CARRIER.setCorrelationId(id);
    }

    public static String getCorrelationId() {
        return CARRIER.getCorrelationId();
    }

    public static void clearCorrelationId() {
        CARRIER.clearCorrelationId();
    }

    // ==================== Request ID ====================

    public static void setRequestId(String id) {
        CARRIER.setRequestId(id);
    }

    public static String getRequestId() {
        return CARRIER.getRequestId();
    }

    public static void clearRequestId() {
        CARRIER.clearRequestId();
    }

    // ==================== User ID ====================

    public static void setUserId(String id) {
        CARRIER.setUserId(id);
    }

    public static String getUserId() {
        return CARRIER.getUserId();
    }

    public static void clearUserId() {
        CARRIER.clearUserId();
    }

    /**
     * Clear all header-related contexts: correlation, request, tenant and user.
     */
    public static void clearHeaders() {
        CARRIER.clearCorrelationId();
        CARRIER.clearRequestId();
        CARRIER.clearTenantId();
        CARRIER.clearUserId();
    }
}
