package com.ejada.kafka_starter.core;

import com.ejada.common.context.ContextManager;
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
    String tenantId = com.ejada.common.tenant.TenantIsolationValidator.requireTenant("EventEnvelope.from");
    com.ejada.common.tenant.TenantIsolationValidator.verifyKafkaOperation("EventEnvelope.from", tenantId);
    return new EventEnvelope<>(
        event,
        Instant.now(),
        tenantId,
        ContextManager.getCorrelationId(),
        "1.0",
        data);
  }
}