package com.ejada.gateway.admin;

import com.ejada.gateway.admin.model.AdminServiceSnapshot;
import com.ejada.gateway.admin.model.AdminServiceState;
import com.ejada.gateway.admin.model.DetailedHealthStatus;
import org.springframework.boot.actuate.autoconfigure.health.Readiness;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Contributes readiness information by validating critical gateway dependencies.
 */
@Component
@Readiness
public class GatewayReadinessIndicator implements ReactiveHealthIndicator {

  private final AdminAggregationService adminAggregationService;

  public GatewayReadinessIndicator(AdminAggregationService adminAggregationService) {
    this.adminAggregationService = adminAggregationService;
  }

  @Override
  public Mono<Health> health() {
    return adminAggregationService.fetchDetailedHealth()
        .map(this::mapToHealth)
        .onErrorResume(ex -> Mono.just(Health.down(ex).build()));
  }

  private Health mapToHealth(DetailedHealthStatus detailed) {
    boolean redisUp = detailed.redis() != null && detailed.redis().isUp();
    boolean downstreamHealthy = detailed.downstreamServices().stream()
        .filter(AdminServiceSnapshot::required)
        .allMatch(snapshot -> snapshot.state() != AdminServiceState.DOWN);
    boolean breakersHealthy = detailed.circuitBreakers().stream()
        .noneMatch(cb -> "open".equalsIgnoreCase(cb.state()));

    Health.Builder builder = (redisUp && downstreamHealthy && breakersHealthy)
        ? Health.up()
        : Health.down();
    builder.withDetail("redis", detailed.redis());
    builder.withDetail("downstreamServices", detailed.downstreamServices());
    builder.withDetail("circuitBreakers", detailed.circuitBreakers());
    return builder.build();
  }
}
