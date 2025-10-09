package com.ejada.gateway.admin;

import com.ejada.gateway.admin.model.AdminServiceSnapshot;
import com.ejada.gateway.admin.model.AdminServiceState;
import com.ejada.gateway.admin.model.DetailedHealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Contributes readiness information by validating critical gateway dependencies.
 */
@Component
public class GatewayReadinessIndicator implements ReactiveHealthIndicator {

  private static final Logger LOGGER = LoggerFactory.getLogger(GatewayReadinessIndicator.class);

  private final AdminAggregationService adminAggregationService;

  public GatewayReadinessIndicator(AdminAggregationService adminAggregationService) {
    this.adminAggregationService = adminAggregationService;
  }

  @Override
  public Mono<Health> health() {
    return adminAggregationService.fetchDetailedHealth()
        .map(this::mapToHealth)
        .doOnError(ex -> LOGGER.error("Health check failed: {}", ex.getMessage(), ex))
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
