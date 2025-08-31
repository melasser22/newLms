package com.common.context;

/**
 * @deprecated Use {@link CorrelationContextUtil} instead. This class exists only
 * for backwards compatibility and will be removed in a future release.
 */
@Deprecated(forRemoval = true, since = "1.0.0")
public final class TraceContextUtil {

    private TraceContextUtil() {
    }

    /**
     * @deprecated Use {@link CorrelationContextUtil#init(String, String)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static void init(String correlationId, String tenantId) {
        CorrelationContextUtil.init(correlationId, tenantId);
    }

    /**
     * @deprecated Use {@link CorrelationContextUtil#getCorrelationId()} instead.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static String getCorrelationId() {
        return CorrelationContextUtil.getCorrelationId();
    }

    /**
     * @deprecated Legacy alias for {@link CorrelationContextUtil#getCorrelationId()}.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static String getTraceId() {
        return CorrelationContextUtil.getCorrelationId();
    }

    /**
     * @deprecated Use {@link CorrelationContextUtil#getTenantId()} instead.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static String getTenantId() {
        return CorrelationContextUtil.getTenantId();
    }

    /**
     * @deprecated Use {@link CorrelationContextUtil#clear()} instead.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static void clear() {
        CorrelationContextUtil.clear();
    }

    /**
     * @deprecated Use {@link CorrelationContextUtil#put(String, String)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static void put(String key, String value) {
        CorrelationContextUtil.put(key, value);
    }

    /**
     * @deprecated Use {@link CorrelationContextUtil#get(String)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.0.0")
    public static String get(String key) {
        return CorrelationContextUtil.get(key);
    }
}
