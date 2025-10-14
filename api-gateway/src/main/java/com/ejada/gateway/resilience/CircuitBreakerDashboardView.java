package com.ejada.gateway.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * View model exposed by the circuit breaker dashboard endpoint. Captures the latest state, failure
 * rate and tenant level metadata gathered by {@link TenantCircuitBreakerMetrics}.
 */
public record CircuitBreakerDashboardView(
    String name,
    TenantCircuitBreakerMetrics.Priority priority,
    CircuitBreaker.State state,
    CircuitBreaker.State previousState,
    double failureRate,
    Instant lastUpdated,
    Instant lastStateTransitionAt,
    Instant lastOpenedAt,
    Instant lastHalfOpenedAt,
    Instant lastClosedAt,
    long fallbackCount,
    String lastTenant,
    Instant lastFallbackAt,
    String lastFallbackType,
    Map<String, Object> lastFallbackMetadata,
    Instant lastRecoveryProbeSuccess,
    Instant lastRecoveryProbeFailure,
    boolean recoveryScheduled,
    Instant recoveryScheduledAt,
    Map<String, Long> tenantImpact,
    List<TenantImpactView> topTenantImpact,
    long impactedTenantCount) {

  public record TenantImpactView(String tenantId, long fallbacks) { }
}
