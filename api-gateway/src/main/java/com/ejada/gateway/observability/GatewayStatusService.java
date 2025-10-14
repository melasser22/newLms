package com.ejada.gateway.observability;

import com.ejada.gateway.resilience.TenantCircuitBreakerMetrics;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;

/**
 * Captures gateway runtime metadata such as start time and circuit breaker state snapshots.
 */
@Service
public class GatewayStatusService implements ApplicationListener<ApplicationReadyEvent> {

  private final String version;
  private final TenantCircuitBreakerMetrics circuitBreakerMetrics;
  private volatile Instant startedAt = Instant.now();

  public GatewayStatusService(@Value("${app.version:unknown}") String version,
      TenantCircuitBreakerMetrics circuitBreakerMetrics) {
    this.version = version;
    this.circuitBreakerMetrics = circuitBreakerMetrics;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    this.startedAt = Instant.now();
  }

  public GatewayStatusResponse snapshot() {
    Instant start = Optional.ofNullable(startedAt).orElseGet(Instant::now);
    long uptime = Duration.between(start, Instant.now()).getSeconds();
    List<GWStatus> circuitBreakers = (circuitBreakerMetrics != null)
        ? circuitBreakerMetrics.snapshotViews().stream()
            .map(view -> new GWStatus(view.name(), view.priority(), view.state().name()))
            .sorted(Comparator
                .comparing(GWStatus::priority)
                .reversed()
                .thenComparing(GWStatus::name))
            .toList()
        : Collections.emptyList();
    List<GatewayStatusResponse.CircuitBreakerState> mapped = circuitBreakers.stream()
        .map(entry -> new GatewayStatusResponse.CircuitBreakerState(entry.name(), entry.priority(), entry.state()))
        .toList();
    return new GatewayStatusResponse(version, start, uptime, mapped);
  }

  private record GWStatus(String name, TenantCircuitBreakerMetrics.Priority priority, String state) { }
}
