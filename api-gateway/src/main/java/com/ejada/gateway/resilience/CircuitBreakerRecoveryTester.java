package com.ejada.gateway.resilience;

import com.ejada.gateway.config.AdminAggregationProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Performs lightweight recovery probes when a circuit breaker transitions to the open state. Once
 * the downstream service responds successfully to the configured health endpoint the scheduled
 * probe is cancelled and the circuit breaker is allowed to recover naturally.
 */
@Component
public class CircuitBreakerRecoveryTester {

  private static final Logger LOGGER = LoggerFactory.getLogger(CircuitBreakerRecoveryTester.class);

  private static final Duration PROBE_INTERVAL = Duration.ofSeconds(15);

  private final AdminAggregationProperties adminProperties;
  private final WebClient.Builder webClientBuilder;
  private final TenantCircuitBreakerMetrics metrics;
  private final ConcurrentMap<String, Disposable> scheduled = new ConcurrentHashMap<>();
  private final Set<String> monitored = ConcurrentHashMap.newKeySet();

  public CircuitBreakerRecoveryTester(AdminAggregationProperties adminProperties,
      WebClient.Builder webClientBuilder,
      TenantCircuitBreakerMetrics metrics) {
    this.adminProperties = Objects.requireNonNull(adminProperties, "adminProperties");
    this.webClientBuilder = Objects.requireNonNull(webClientBuilder, "webClientBuilder");
    this.metrics = Objects.requireNonNull(metrics, "metrics");
  }

  public void monitor(CircuitBreaker circuitBreaker) {
    if (circuitBreaker == null) {
      return;
    }
    if (!monitored.add(circuitBreaker.getName())) {
      return;
    }
    circuitBreaker.getEventPublisher().onStateTransition(event -> handleTransition(circuitBreaker, event));
  }

  private void handleTransition(CircuitBreaker circuitBreaker, CircuitBreakerOnStateTransitionEvent event) {
    CircuitBreaker.State toState = event.getStateTransition().getToState();
    String name = circuitBreaker.getName();
    switch (toState) {
      case OPEN -> schedule(circuitBreaker);
      case CLOSED -> {
        cancel(name);
        metrics.markRecoveryIdle(name);
      }
      case HALF_OPEN -> runProbe(circuitBreaker).subscribe();
      default -> {
        // no-op
      }
    }
  }

  private void schedule(CircuitBreaker circuitBreaker) {
    String circuitBreakerName = circuitBreaker.getName();
    cancel(circuitBreakerName);
    TenantCircuitBreakerMetrics.Priority priority = metrics.priorityOf(circuitBreakerName);
    metrics.markRecoveryScheduled(circuitBreakerName);
    Disposable disposable = Flux.interval(Duration.ZERO, PROBE_INTERVAL, Schedulers.boundedElastic())
        .flatMap(ignore -> runProbe(circuitBreaker))
        .subscribe();
    scheduled.put(circuitBreakerName, disposable);
    LOGGER.info("Scheduled proactive recovery probe for {} (priority={}) every {}", circuitBreakerName,
        priority, PROBE_INTERVAL);
  }

  private Mono<Void> runProbe(CircuitBreaker circuitBreaker) {
    String circuitBreakerName = circuitBreaker.getName();
    Optional<AdminAggregationProperties.Service> serviceOptional = locateService(circuitBreakerName);
    if (serviceOptional.isEmpty()) {
      metrics.markRecoveryProbe(circuitBreakerName, false);
      LOGGER.debug("Skipping recovery probe for {} as no admin service mapping was found", circuitBreakerName);
      return Mono.<Void>empty();
    }
    AdminAggregationProperties.Service service = serviceOptional.get();
    Duration timeout = service.resolveTimeout(adminProperties.getAggregation().getTimeout());
    WebClient client = webClientBuilder.clone().baseUrl(service.getUri().toString()).build();
    return client.get()
        .uri(service.getHealthPath())
        .headers(headers -> service.getHeaders().forEach(headers::add))
        .retrieve()
        .toBodilessEntity()
        .timeout(timeout)
        .then(Mono.fromRunnable(() -> {
          LOGGER.info("Recovery probe for {} succeeded", circuitBreakerName);
          metrics.markRecoveryProbe(circuitBreakerName, true);
          cancel(circuitBreakerName);
          attemptHalfOpenTransition(circuitBreaker);
        })).then()
        .onErrorResume(ex -> {
          LOGGER.debug("Recovery probe for {} failed: {}", circuitBreakerName, ex.getMessage());
          metrics.markRecoveryProbe(circuitBreakerName, false);
          return Mono.<Void>empty();
        });
  }

  private void attemptHalfOpenTransition(CircuitBreaker circuitBreaker) {
    if (circuitBreaker.getState() != CircuitBreaker.State.OPEN) {
      return;
    }
    try {
      circuitBreaker.transitionToHalfOpenState();
      LOGGER.info("Transitioned circuit breaker {} to HALF_OPEN after successful probe", circuitBreaker.getName());
    } catch (Exception ex) {
      LOGGER.debug("Failed to transition circuit breaker {} to HALF_OPEN: {}", circuitBreaker.getName(), ex.getMessage());
    }
  }

  private Optional<AdminAggregationProperties.Service> locateService(String circuitBreakerName) {
    return adminProperties.getAggregation().getServices().stream()
        .filter(service -> circuitBreakerName.equalsIgnoreCase(service.getId()))
        .findFirst();
  }

  private void cancel(String circuitBreakerName) {
    Disposable disposable = scheduled.remove(circuitBreakerName);
    if (disposable != null) {
      disposable.dispose();
      LOGGER.debug("Cancelled recovery probe for {}", circuitBreakerName);
    }
  }
}
