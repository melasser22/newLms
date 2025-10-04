package com.ejada.gateway.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;

/**
 * Helper component exposing gateway-specific security counters.
 */
public class GatewaySecurityMetrics {

  private final MeterRegistry meterRegistry;

  public GatewaySecurityMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
  }

  public void incrementBlocked(String cause, String tenantId) {
    Counter counter = meterRegistry.counter("gateway.security.blocked",
        "cause", Objects.toString(cause, "unknown"),
        "tenant", Objects.toString(tenantId, "unknown"));
    counter.increment();
  }

  public void incrementApiKeyValidated(String tenantId) {
    Counter counter = meterRegistry.counter("gateway.security.api_key_validated",
        "tenant", Objects.toString(tenantId, "unknown"));
    counter.increment();
  }
}
