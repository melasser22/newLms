package com.ejada.gateway.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Registers tenant aware tracking for circuit breakers managed by the shared {@link
 * io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry}. By subscribing to registry events we can
 * automatically bind gateway specific metrics and trigger the recovery tester whenever new circuit
 * breakers are created or replaced.
 */
@Component
public class TenantAwareCircuitBreakerRegistryConsumer implements RegistryEventConsumer<CircuitBreaker> {

  private final TenantCircuitBreakerMetrics metrics;
  private final CircuitBreakerRecoveryTester recoveryTester;

  public TenantAwareCircuitBreakerRegistryConsumer(TenantCircuitBreakerMetrics metrics,
      CircuitBreakerRecoveryTester recoveryTester) {
    this.metrics = Objects.requireNonNull(metrics, "metrics");
    this.recoveryTester = Objects.requireNonNull(recoveryTester, "recoveryTester");
  }

  @Override
  public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> event) {
    CircuitBreaker circuitBreaker = event.getAddedEntry();
    metrics.bind(circuitBreaker);
    recoveryTester.monitor(circuitBreaker);
  }

  @Override
  public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> event) {
    CircuitBreaker circuitBreaker = event.getRemovedEntry();
    if (circuitBreaker != null) {
      metrics.markRecoveryIdle(circuitBreaker.getName());
    }
  }

  @Override
  public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> event) {
    CircuitBreaker circuitBreaker = event.getNewEntry();
    metrics.bind(circuitBreaker);
    recoveryTester.monitor(circuitBreaker);
  }
}
