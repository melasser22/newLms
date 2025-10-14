package com.ejada.gateway.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregate view returned by {@link CircuitBreakerDashboardController} combining high level
 * statistics with the detailed {@link CircuitBreakerDashboardView} list. The aggregate also
 * surfaces tenant impact analysis so dashboards can highlight which tenants are most affected by
 * downstream instability.
 */
public record CircuitBreakerDashboardAggregate(
    Instant generatedAt,
    long total,
    long open,
    long halfOpen,
    long closed,
    long criticalOpen,
    long totalFallbacks,
    long impactedTenants,
    Map<TenantCircuitBreakerMetrics.Priority, Long> priorityBreakdown,
    List<CircuitBreakerDashboardView.TenantImpactView> topTenantImpact,
    List<CircuitBreakerDashboardView> circuitBreakers) {

  public static CircuitBreakerDashboardAggregate from(List<CircuitBreakerDashboardView> views) {
    if (views == null || views.isEmpty()) {
      Map<TenantCircuitBreakerMetrics.Priority, Long> breakdown = new EnumMap<>(TenantCircuitBreakerMetrics.Priority.class);
      for (TenantCircuitBreakerMetrics.Priority priority : TenantCircuitBreakerMetrics.Priority.values()) {
        breakdown.put(priority, 0L);
      }
      return new CircuitBreakerDashboardAggregate(
          Instant.now(),
          0,
          0,
          0,
          0,
          0,
          0,
          0,
          Map.copyOf(breakdown),
          List.of(),
          List.of());
    }

    Instant timestamp = Instant.now();
    long open = 0;
    long halfOpen = 0;
    long closed = 0;
    long criticalOpen = 0;
    long totalFallbacks = 0;
    Map<TenantCircuitBreakerMetrics.Priority, Long> breakdown = new EnumMap<>(TenantCircuitBreakerMetrics.Priority.class);
    for (TenantCircuitBreakerMetrics.Priority priority : TenantCircuitBreakerMetrics.Priority.values()) {
      breakdown.put(priority, 0L);
    }
    Map<String, Long> tenantImpact = new LinkedHashMap<>();

    for (CircuitBreakerDashboardView view : views) {
      CircuitBreaker.State state = view.state();
      switch (state) {
        case OPEN -> open++;
        case HALF_OPEN -> halfOpen++;
        case CLOSED -> closed++;
        default -> {
          // ignore other states for summary counts
        }
      }
      if (view.priority() == TenantCircuitBreakerMetrics.Priority.CRITICAL
          && (state == CircuitBreaker.State.OPEN || state == CircuitBreaker.State.HALF_OPEN)) {
        criticalOpen++;
      }
      totalFallbacks += view.fallbackCount();
      breakdown.computeIfPresent(view.priority(), (key, value) -> value + 1);
      breakdown.putIfAbsent(view.priority(), 1L);
      view.tenantImpact().forEach((tenant, count) -> tenantImpact.merge(tenant, count, Long::sum));
    }

    List<CircuitBreakerDashboardView.TenantImpactView> topImpact = tenantImpact.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(10)
        .map(entry -> new CircuitBreakerDashboardView.TenantImpactView(entry.getKey(), entry.getValue()))
        .toList();

    return new CircuitBreakerDashboardAggregate(
        timestamp,
        views.size(),
        open,
        halfOpen,
        closed,
        criticalOpen,
        totalFallbacks,
        tenantImpact.size(),
        Map.copyOf(breakdown),
        List.copyOf(topImpact),
        List.copyOf(views));
  }
}
