package com.ejada.gateway.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Tracks tenant aware circuit breaker metrics and exposes consolidated state for the dashboard
 * endpoint. The component binds micrometer gauges that surface the gateway specific metrics
 * requested by operations teams while keeping additional metadata in memory for quick inspection.
 */
@Component
public class TenantCircuitBreakerMetrics {

  public enum Priority {
    CRITICAL,
    NON_CRITICAL;

    public static Priority from(@Nullable String value) {
      if (!StringUtils.hasText(value)) {
        return NON_CRITICAL;
      }
      try {
        return Priority.valueOf(value.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ex) {
        return NON_CRITICAL;
      }
    }
  }

  private final MeterRegistry meterRegistry;
  private final ConcurrentMap<String, CircuitBreakerInsight> insights = new ConcurrentHashMap<>();
  private final Set<String> registeredMeters = ConcurrentHashMap.newKeySet();
  private final Set<String> monitoredBreakers = ConcurrentHashMap.newKeySet();

  public TenantCircuitBreakerMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
  }

  public void bind(CircuitBreaker circuitBreaker) {
    if (circuitBreaker == null) {
      return;
    }
    String name = circuitBreaker.getName();
    if (!monitoredBreakers.add(name)) {
      return;
    }
    insights.computeIfAbsent(name, CircuitBreakerInsight::initial);
    registerMeters(circuitBreaker);
    updateSnapshot(circuitBreaker);
    circuitBreaker.getEventPublisher()
        .onStateTransition(event -> updateSnapshot(circuitBreaker))
        .onReset(event -> updateSnapshot(circuitBreaker))
        .onCallNotPermitted(event -> updateSnapshot(circuitBreaker))
        .onFailureRateExceeded(event -> updateSnapshot(circuitBreaker));
  }

  public void registerPriority(String circuitBreakerName, Priority priority) {
    insights.compute(circuitBreakerName, (key, existing) -> {
      CircuitBreakerInsight base = (existing != null) ? existing : CircuitBreakerInsight.initial(key);
      return base.withPriority(priority);
    });
  }

  public void recordFallback(String circuitBreakerName,
      @Nullable String tenantId,
      String fallbackType,
      Map<String, Object> metadata) {
    String resolvedTenant = StringUtils.hasText(tenantId) ? tenantId.trim() : "unknown";
    insights.compute(circuitBreakerName, (key, existing) -> {
      CircuitBreakerInsight base = (existing != null) ? existing : CircuitBreakerInsight.initial(key);
      return base.recordFallback(resolvedTenant, fallbackType, metadata, Instant.now());
    });
  }

  public void markRecoveryScheduled(String circuitBreakerName) {
    insights.computeIfPresent(circuitBreakerName,
        (key, snapshot) -> snapshot.markRecoveryScheduled(Instant.now()));
  }

  public void markRecoveryProbe(String circuitBreakerName, boolean success) {
    insights.computeIfPresent(circuitBreakerName,
        (key, snapshot) -> snapshot.markRecoveryProbe(success, Instant.now()));
  }

  public void markRecoveryIdle(String circuitBreakerName) {
    insights.computeIfPresent(circuitBreakerName,
        (key, snapshot) -> snapshot.markRecoveryIdle(Instant.now()));
  }

  public List<CircuitBreakerDashboardView> snapshotViews() {
    List<CircuitBreakerDashboardView> views = new ArrayList<>();
    insights.forEach((name, snapshot) -> views.add(snapshot.toView()));
    views.sort((a, b) -> {
      int priorityCompare = b.priority().compareTo(a.priority());
      if (priorityCompare != 0) {
        return priorityCompare;
      }
      return a.name().compareToIgnoreCase(b.name());
    });
    return views;
  }

  public Priority priorityOf(String circuitBreakerName) {
    CircuitBreakerInsight snapshot = insights.get(circuitBreakerName);
    return snapshot != null ? snapshot.priority() : Priority.NON_CRITICAL;
  }

  double stateValue(String circuitBreakerName, CircuitBreaker.State state) {
    CircuitBreakerInsight snapshot = insights.get(circuitBreakerName);
    if (snapshot == null) {
      return 0.0d;
    }
    return snapshot.state() == state ? 1.0d : 0.0d;
  }

  double failureRate(String circuitBreakerName) {
    CircuitBreakerInsight snapshot = insights.get(circuitBreakerName);
    if (snapshot == null) {
      return 0.0d;
    }
    return snapshot.failureRate();
  }

  private void updateSnapshot(CircuitBreaker circuitBreaker) {
    insights.compute(circuitBreaker.getName(), (key, existing) -> {
      CircuitBreakerInsight base = (existing != null) ? existing : CircuitBreakerInsight.initial(key);
      return base.update(circuitBreaker.getState(), circuitBreaker.getMetrics().getFailureRate(), Instant.now());
    });
  }

  private void registerMeters(CircuitBreaker circuitBreaker) {
    EnumSet<CircuitBreaker.State> states = EnumSet.allOf(CircuitBreaker.State.class);
    for (CircuitBreaker.State state : states) {
      String key = circuitBreaker.getName() + ':' + state.name();
      if (registeredMeters.add(key)) {
        Gauge.builder("gateway_circuit_breaker_state", this,
                metrics -> metrics.stateValue(circuitBreaker.getName(), state))
            .description("State of resilience4j circuit breakers exposed by the gateway")
            .tags("serviceName", circuitBreaker.getName(),
                "state", state.name().toLowerCase(Locale.ROOT))
            .register(meterRegistry);
      }
    }
    String failureRateKey = circuitBreaker.getName() + ":failureRate";
    if (registeredMeters.add(failureRateKey)) {
      Gauge.builder("gateway_circuit_breaker_failure_rate", this,
              metrics -> metrics.failureRate(circuitBreaker.getName()))
          .description("Failure rate percentage for gateway circuit breakers")
          .tags("serviceName", circuitBreaker.getName())
          .register(meterRegistry);
    }
  }

  private record CircuitBreakerInsight(
      String name,
      Priority priority,
      CircuitBreaker.State state,
      @Nullable CircuitBreaker.State previousState,
      double failureRate,
      Instant lastUpdated,
      @Nullable Instant lastStateTransitionAt,
      @Nullable Instant lastOpenedAt,
      @Nullable Instant lastHalfOpenedAt,
      @Nullable Instant lastClosedAt,
      long fallbackCount,
      @Nullable String lastTenant,
      @Nullable Instant lastFallbackAt,
      @Nullable String lastFallbackType,
      Map<String, Object> lastFallbackMetadata,
      @Nullable Instant lastRecoveryProbeSuccess,
      @Nullable Instant lastRecoveryProbeFailure,
      boolean recoveryScheduled,
      Instant recoveryScheduledAt,
      Map<String, Long> tenantImpact) {

    private static CircuitBreakerInsight initial(String name) {
      Instant now = Instant.now();
      return new CircuitBreakerInsight(name, Priority.NON_CRITICAL, CircuitBreaker.State.CLOSED, null, 0.0d,
          now, now, null, null, now, 0, null, null, null, Map.of(), null, null, false, null, Map.of());
    }

    private CircuitBreakerInsight withPriority(Priority priority) {
      return new CircuitBreakerInsight(name, Optional.ofNullable(priority).orElse(this.priority), state,
          previousState, failureRate, lastUpdated, lastStateTransitionAt, lastOpenedAt, lastHalfOpenedAt,
          lastClosedAt, fallbackCount, lastTenant, lastFallbackAt, lastFallbackType,
          lastFallbackMetadata, lastRecoveryProbeSuccess, lastRecoveryProbeFailure, recoveryScheduled,
          recoveryScheduledAt, tenantImpact);
    }

    private CircuitBreakerInsight update(CircuitBreaker.State newState, float newFailureRate, Instant timestamp) {
      double rate = Double.isFinite(newFailureRate) && newFailureRate >= 0 ? newFailureRate : 0.0d;
      boolean changed = newState != state;
      CircuitBreaker.State resolvedPrevious = changed ? state : previousState;
      Instant transitionAt = changed ? timestamp : lastStateTransitionAt;
      Instant openedAt = lastOpenedAt;
      Instant halfOpenedAt = lastHalfOpenedAt;
      Instant closedAt = lastClosedAt;
      if (changed) {
        switch (newState) {
          case OPEN -> openedAt = timestamp;
          case HALF_OPEN -> halfOpenedAt = timestamp;
          case CLOSED -> closedAt = timestamp;
          default -> {
            // no-op
          }
        }
      }
      return new CircuitBreakerInsight(name, priority, newState, resolvedPrevious, rate, timestamp, transitionAt,
          openedAt, halfOpenedAt, closedAt, fallbackCount, lastTenant, lastFallbackAt, lastFallbackType,
          lastFallbackMetadata, lastRecoveryProbeSuccess, lastRecoveryProbeFailure, recoveryScheduled,
          recoveryScheduledAt, tenantImpact);
    }

    private CircuitBreakerInsight recordFallback(String tenant, String type,
        Map<String, Object> metadata, Instant timestamp) {
      Map<String, Object> details = (metadata == null || metadata.isEmpty()) ? Map.of() : Map.copyOf(metadata);
      Map<String, Long> distribution;
      if (tenantImpact == null || tenantImpact.isEmpty()) {
        distribution = new LinkedHashMap<>();
      } else {
        distribution = new LinkedHashMap<>(tenantImpact);
      }
      distribution.merge(tenant, 1L, Long::sum);
      return new CircuitBreakerInsight(name, priority, state, previousState, failureRate, timestamp,
          lastStateTransitionAt, lastOpenedAt, lastHalfOpenedAt, lastClosedAt,
          fallbackCount + 1, tenant, timestamp, type, details, lastRecoveryProbeSuccess,
          lastRecoveryProbeFailure, recoveryScheduled, recoveryScheduledAt, Map.copyOf(distribution));
    }

    private CircuitBreakerInsight markRecoveryScheduled(Instant timestamp) {
      return new CircuitBreakerInsight(name, priority, state, previousState, failureRate, timestamp, lastStateTransitionAt,
          lastOpenedAt, lastHalfOpenedAt, lastClosedAt, fallbackCount, lastTenant,
          lastFallbackAt, lastFallbackType, lastFallbackMetadata, lastRecoveryProbeSuccess,
          lastRecoveryProbeFailure, true, timestamp, tenantImpact);
    }

    private CircuitBreakerInsight markRecoveryProbe(boolean success, Instant timestamp) {
      if (success) {
        return new CircuitBreakerInsight(name, priority, state, previousState, failureRate, timestamp, lastStateTransitionAt,
            lastOpenedAt, lastHalfOpenedAt, lastClosedAt, fallbackCount,
            lastTenant, lastFallbackAt, lastFallbackType, lastFallbackMetadata, timestamp,
            lastRecoveryProbeFailure, false, recoveryScheduledAt, tenantImpact);
      }
      return new CircuitBreakerInsight(name, priority, state, previousState, failureRate, timestamp, lastStateTransitionAt,
          lastOpenedAt, lastHalfOpenedAt, lastClosedAt, fallbackCount, lastTenant,
          lastFallbackAt, lastFallbackType, lastFallbackMetadata, lastRecoveryProbeSuccess, timestamp,
          true, recoveryScheduledAt, tenantImpact);
    }

    private CircuitBreakerInsight markRecoveryIdle(Instant timestamp) {
      return new CircuitBreakerInsight(name, priority, state, previousState, failureRate, timestamp, lastStateTransitionAt,
          lastOpenedAt, lastHalfOpenedAt, lastClosedAt, fallbackCount, lastTenant,
          lastFallbackAt, lastFallbackType, lastFallbackMetadata, lastRecoveryProbeSuccess,
          lastRecoveryProbeFailure, false, recoveryScheduledAt, tenantImpact);
    }

    private CircuitBreakerDashboardView toView() {
      Map<String, Long> impact = tenantImpact != null ? Map.copyOf(tenantImpact) : Map.of();
      List<CircuitBreakerDashboardView.TenantImpactView> topImpact = impact.entrySet().stream()
          .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
          .limit(5)
          .map(entry -> new CircuitBreakerDashboardView.TenantImpactView(entry.getKey(), entry.getValue()))
          .collect(Collectors.toList());
      return new CircuitBreakerDashboardView(name, priority, state, previousState, failureRate, lastUpdated,
          lastStateTransitionAt, lastOpenedAt, lastHalfOpenedAt, lastClosedAt, fallbackCount,
          lastTenant, lastFallbackAt, lastFallbackType, lastFallbackMetadata, lastRecoveryProbeSuccess,
          lastRecoveryProbeFailure, recoveryScheduled, recoveryScheduledAt, impact,
          List.copyOf(topImpact), impact.size());
    }
  }
}
