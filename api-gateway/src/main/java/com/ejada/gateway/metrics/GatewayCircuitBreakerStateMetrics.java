package com.ejada.gateway.metrics;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Publishes circuit breaker states as gauge rows tagged by service name and state.
 */
public class GatewayCircuitBreakerStateMetrics implements MeterBinder {

  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final ReentrantLock lock = new ReentrantLock();
  private MultiGauge multiGauge;

  public GatewayCircuitBreakerStateMetrics(CircuitBreakerRegistry circuitBreakerRegistry) {
    this.circuitBreakerRegistry = Objects.requireNonNull(circuitBreakerRegistry, "circuitBreakerRegistry");
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    this.multiGauge = MultiGauge.builder("gateway.circuit_breaker.state")
        .description("Current circuit breaker states exposed by the gateway")
        .register(registry);
    circuitBreakerRegistry.getEventPublisher()
        .onStateTransition(event -> refresh())
        .onEntryAdded(event -> refresh())
        .onEntryRemoved(event -> refresh());
    refresh();
  }

  private void refresh() {
    lock.lock();
    try {
      if (multiGauge == null) {
        return;
      }
      List<MultiGauge.Row<?>> rows = circuitBreakerRegistry.getAllCircuitBreakers().stream()
          .map(this::toRow)
          .toList();
      multiGauge.register(rows, true);
    } finally {
      lock.unlock();
    }
  }

  private MultiGauge.Row<?> toRow(CircuitBreaker circuitBreaker) {
    CircuitBreaker.State state = circuitBreaker.getState();
    double value = switch (state) {
      case CLOSED -> 0.0d;
      case HALF_OPEN -> 0.5d;
      case OPEN -> 1.0d;
      default -> -1.0d;
    };
    Tags tags = Tags.of("serviceName", circuitBreaker.getName(), "state", state.name());
    return MultiGauge.Row.of(tags, value);
  }
}
