package com.common.context;

import com.common.constants.HeaderNames;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Consolidated correlation context utility. This class lives in the {@code com.common.context}
 * package and should be used for putting and getting values from the SLF4J MDC.
 *
 * <p>All correlation handling should be done through this class.</p>

 */
public final class CorrelationContextUtil {

    /** MDC key for the correlation identifier. */
    public static final String CORRELATION_ID = HeaderNames.CORRELATION_ID;

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
            MDC.put(HeaderNames.X_TENANT_ID, tenantId);
        }
    }

    /**
     * Retrieve the current correlation identifier. If none exists in the MDC a new

     * identifier is generated, stored and returned. This guarantees that callers
     * always receive a non-null correlation id even if no filter initialized it.
     *
     * @return the existing or newly generated correlation identifier
     */
    public static String getCorrelationId() {
        String cid = MDC.get(CORRELATION_ID);
        if (cid == null || cid.isBlank()) {
            cid = UUID.randomUUID().toString();
            MDC.put(CORRELATION_ID, cid);
        }
        return cid;
    }

    /**
     * @return the current tenant identifier from MDC or {@code null}
     */
    public static String getTenantId() {
        return MDC.get(HeaderNames.X_TENANT_ID);
    }

    /**
     * Clear correlation and tenant identifiers from MDC.
     */
    public static void clear() {
        MDC.remove(CORRELATION_ID);
        MDC.remove(HeaderNames.X_TENANT_ID);
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