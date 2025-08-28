package com.common.context;

import com.common.constants.HeaderNames;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Consolidated trace context utility. This class lives in the {@code com.common.context}
 * package and should be used for putting and getting values from the SLF4J MDC.
 *
 * <p>It replaces the duplicate implementations previously found in
 * {@code com.common.logging.TraceContextUtil} and {@code com.common.util.TraceContextUtil}.
 * The old classes delegate to this implementation and are marked as deprecated to
 * preserve backward compatibility.</p>
 */
public final class TraceContextUtil {

    /** MDC key for the trace identifier. */
    public static final String TRACE_ID = "traceId";

    private TraceContextUtil() {
        // utility class
    }

    /**
     * Initialize trace context. Generates a UUID if no traceId is provided.
     *
     * @param traceId  trace identifier (optional)
     * @param tenantId tenant identifier (optional)
     */
    public static void init(String traceId, String tenantId) {
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(TRACE_ID, traceId);
        if (tenantId != null && !tenantId.isBlank()) {
            MDC.put(HeaderNames.TENANT_ID, tenantId);
        }
    }

    /**
     * @return the current trace identifier from MDC or {@code null}
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * @return the current tenant identifier from MDC or {@code null}
     */
    public static String getTenantId() {
        return MDC.get(HeaderNames.TENANT_ID);
    }

    /**
     * Clear trace and tenant identifiers from MDC.
     */
    public static void clear() {
        MDC.remove(TRACE_ID);
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