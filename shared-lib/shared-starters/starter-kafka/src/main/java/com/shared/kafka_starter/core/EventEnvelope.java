package com.shared.kafka_starter.core;

import com.common.context.ContextManager;
import java.time.Instant;

public record EventEnvelope<T>(
    String event,       // e.g. points.earned
    Instant timestamp,  // event time
    String tenantId,
    String correlationId,
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
        "1.0",
        data);
  }
}