package com.ejada.gateway.observability;

import com.ejada.gateway.resilience.TenantCircuitBreakerMetrics;
import java.time.Instant;
import java.util.List;

/**
 * View model rendered by the gateway status endpoint summarising runtime metadata.
 */
public record GatewayStatusResponse(
    String version,
    Instant startedAt,
    long uptimeSeconds,
    List<CircuitBreakerState> circuitBreakers) {

  public record CircuitBreakerState(String name, TenantCircuitBreakerMetrics.Priority priority, String state) { }
}
