package com.common.context;

import com.common.constants.HeaderNames;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Consolidated correlation context utility. This class lives in the {@code com.common.context}
 * package and should be used for putting and getting values from the SLF4J MDC.
 *
 * <p>It replaces the duplicate implementations previously found in
 * {@code com.common.logging.TraceContextUtil} and {@code com.common.util.TraceContextUtil}.
 * The old classes delegate to this implementation and are marked as deprecated to
 * preserve backward compatibility.</p>
 */
public final class CorrelationContextUtil {

    /** MDC key for the correlation identifier. */
    public static final String CORRELATION_ID = "correlationId";

    private CorrelationContextUtil() {
        // utility class
    }

    /**
     * Initialize correlation context. Generates a UUID if no correlation id is provided.
     *
     * @param correlationId correlation identifier (optional)
     * @param tenantId      tenant identifier (optional)
     */
    public static void init(String correlationId, String tenantId) {
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(CORRELATION_ID, correlationId);
        if (tenantId != null && !tenantId.isBlank()) {
            MDC.put(HeaderNames.TENANT_ID, tenantId);
        }
    }

    /**
     * @return the current correlation identifier from MDC or {@code null}
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID);
    }

    /**
     * @return the current tenant identifier from MDC or {@code null}
     */
    public static String getTenantId() {
        return MDC.get(HeaderNames.TENANT_ID);
    }

    /**
     * Clear correlation and tenant identifiers from MDC.
     */
    public static void clear() {
        MDC.remove(CORRELATION_ID);
        MDC.remove(HeaderNames.TENANT_ID);
    }

    /**
     * Put a custom key/value into MDC.
     *
     * @param key   MDC key
     * @param value value to associate with the key
     */
    public static void put(String key, String value) {
        MDC.put(key, value);
    }

    /**
     * Get a custom value from MDC.
     *
     * @param key MDC key
     * @return value associated with the key or {@code null}
     */
    public static String get(String key) {
        return MDC.get(key);
    }
}