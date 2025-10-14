package com.ejada.common.tenant;

import com.ejada.common.context.ContextManager;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central guard that enforces tenant isolation across infrastructure layers. The
 * validator is intentionally lightweight so it can be used by data access,
 * messaging, and caching helpers without introducing additional dependencies.
 */
public final class TenantIsolationValidator {

    private static final Logger log = LoggerFactory.getLogger(TenantIsolationValidator.class);

    private TenantIsolationValidator() {
        // utility class
    }

    /**
     * Ensure a tenant identifier is available in the current {@link ContextManager}
     * and return it as a String. Throws {@link IllegalStateException} if the
     * tenant context is missing.
     */
    public static String requireTenant(String operation) {
        String tenant = normalize(ContextManager.Tenant.get());
        if (tenant == null) {
            throw new IllegalStateException("Tenant context is required for " + operation);
        }
        return tenant;
    }

    /**
     * Resolve the tenant identifier as a {@link UUID}, throwing if the identifier
     * is not present or cannot be parsed.
     */
    public static UUID requireTenantUuid(String operation) {
        String tenant = requireTenant(operation);
        try {
            return UUID.fromString(tenant);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Tenant context for " + operation + " is not a valid UUID", ex);
        }
    }

    /**
     * Non-throwing lookup that returns an {@link Optional} containing the current
     * tenant identifier if one is present and non-blank.
     */
    public static Optional<String> currentTenant() {
        return Optional.ofNullable(normalize(ContextManager.Tenant.get()));
    }

    /**
     * Resolve the current tenant identifier or fall back to the shared "public"
     * segment when no tenant scope exists. The fallback keeps cache key
     * generation predictable while still signalling that the data is global.
     */
    public static String currentTenantOrPublic() {
        return currentTenant().orElse("public");
    }

    /**
     * Record that a database operation is executing for the given tenant. The
     * method primarily exists to provide consistent logging hooks.
     */
    public static void verifyDatabaseAccess(String operation, UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId");
        if (log.isTraceEnabled()) {
            log.trace("Database operation '{}' scoped to tenant {}", operation, tenantId);
        }
    }

    /**
     * Record that a Redis operation is executing for the given tenant.
     */
    public static void verifyRedisOperation(String operation, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Tenant context is required for Redis operation " + operation);
        }
        if (log.isTraceEnabled()) {
            log.trace("Redis operation '{}' scoped to tenant {}", operation, tenantId);
        }
    }

    /**
     * Record that a Kafka message is being produced for the given tenant.
     */
    public static void verifyKafkaOperation(String operation, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Tenant context is required for Kafka operation " + operation);
        }
        if (log.isTraceEnabled()) {
            log.trace("Kafka operation '{}' scoped to tenant {}", operation, tenantId);
        }
    }

    /**
     * Normalise tenant identifiers to guard against accidental whitespace.
     */
    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}

