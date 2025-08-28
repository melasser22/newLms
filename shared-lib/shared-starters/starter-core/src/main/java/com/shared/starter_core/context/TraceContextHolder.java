
package com.shared.starter_core.context;

public class TraceContextHolder {
    private static final ThreadLocal<String> TRACE_CONTEXT = new ThreadLocal<>();

    public static void setTraceId(String traceId) {
        TRACE_CONTEXT.set(traceId);
    }

    public static String getTraceId() {
        return TRACE_CONTEXT.get();
    }

    public static void clear() {
        TRACE_CONTEXT.remove();
    }
}
