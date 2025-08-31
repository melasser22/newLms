package com.shared.kafka_starter.core;

import com.common.context.ContextManager;
import com.common.context.TraceContextUtil;
import java.time.Instant;

public record EventEnvelope<T>(
    String event,       // e.g. points.earned
    Instant timestamp,  // event time
    String tenantId,
    String correlationId,
    String traceId,
    String schemaVersion,
    T data
) {
  /**
   * Convenience factory that pulls standard header values from the current
   * thread context.
   */
  public static <T> EventEnvelope<T> from(String event, T data) {
    return new EventEnvelope<>(
        event,
        Instant.now(),
        ContextManager.Tenant.get(),
        ContextManager.getCorrelationId(),
        TraceContextUtil.getTraceId(),
        "1.0",
        data);
  }
}