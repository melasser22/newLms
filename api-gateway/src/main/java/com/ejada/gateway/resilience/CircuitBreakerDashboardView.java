package com.ejada.gateway.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Instant;
import java.util.Map;

/**
 * View model exposed by the circuit breaker dashboard endpoint. Captures the latest state, failure
 * rate and tenant level metadata gathered by {@link TenantCircuitBreakerMetrics}.
 */
public record CircuitBreakerDashboardView(
    String name,
    TenantCircuitBreakerMetrics.Priority priority,
    CircuitBreaker.State state,
    double failureRate,
    Instant lastUpdated,
    long fallbackCount,
    String lastTenant,
    Instant lastFallbackAt,
    String lastFallbackType,
    Map<String, Object> lastFallbackMetadata,
    Instant lastRecoveryProbeSuccess,
    Instant lastRecoveryProbeFailure,
    boolean recoveryScheduled,
    Instant recoveryScheduledAt) {
}
