package com.ejada.gateway.resilience;

import com.ejada.common.dto.BaseResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST endpoint exposing aggregated circuit breaker insights for observability dashboards.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class CircuitBreakerDashboardController {

  private final TenantCircuitBreakerMetrics metrics;

  public CircuitBreakerDashboardController(TenantCircuitBreakerMetrics metrics) {
    this.metrics = metrics;
  }

  @GetMapping("/circuit-breakers")
  public Mono<BaseResponse<List<CircuitBreakerDashboardView>>> circuitBreakers() {
    return Mono.fromSupplier(metrics::snapshotViews)
        .map(data -> BaseResponse.success("Gateway circuit breaker dashboard", data));
  }
}
