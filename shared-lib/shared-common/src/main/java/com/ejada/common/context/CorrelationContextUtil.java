package com.ejada.common.context;

import com.ejada.common.constants.HeaderNames;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility for working with correlation identifiers inside the SLF4J MDC.
 * <p>
 * The utility intentionally focuses on correlation data only – tenant/user MDC
 * management lives in their respective helpers to avoid accidental coupling.
 */
public final class CorrelationContextUtil {

    /** Canonical MDC key for correlation identifiers. */
    public static final String CORRELATION_ID = HeaderNames.CORRELATION_ID;

    private CorrelationContextUtil() {
        // utility class
    }

    /**
     * Initialise the MDC with the provided correlation identifier. When the
     * value is {@code null} or blank, a random UUID is generated and stored.
     *
     * @param correlationId suggested correlation identifier (optional)
     * @return the correlation identifier stored in MDC
     */
    public static String init(String correlationId) {
        String cid = correlationId;
        if (cid == null || cid.isBlank()) {
            cid = UUID.randomUUID().toString();
        }
        MDC.put(CORRELATION_ID, cid);
        return cid;
    }

    /**
     * Backwards compatible overload that ignores the tenant identifier. Callers
     * should migrate to {@link #init(String)} and the dedicated tenant helpers.
     */
    @Deprecated(since = "1.6.0", forRemoval = true)
    public static String init(String correlationId, String ignoredTenantId) {
        return init(correlationId);
    }

    /**
     * Store the provided correlation identifier in MDC. Passing {@code null}
     * clears the entry.
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            MDC.remove(CORRELATION_ID);
        } else {
            MDC.put(CORRELATION_ID, correlationId);
        }
    }

    /**
     * Retrieve the current correlation identifier, generating a new one when
     * missing. This guarantees a non-null value for log statements.
     */
    public static String getCorrelationId() {
        String cid = MDC.get(CORRELATION_ID);
        if (cid == null || cid.isBlank()) {
            cid = init(null);
        }
        return cid;
    }

    /** Remove the correlation identifier from MDC. */
    public static void clear() {
        MDC.remove(CORRELATION_ID);
    }

    /**
     * Put a custom key/value into MDC.
     *
     * @param key   MDC key
     * @param value value to associate with the key
     */
    public static void put(String key, String value) {
        if (key == null) {
            return;
        }
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    /**
     * Get a custom value from MDC.
     *
     * @param key MDC key
     * @return value associated with the key or {@code null}
     */
    public static String get(String key) {
        return key == null ? null : MDC.get(key);
    }
}
