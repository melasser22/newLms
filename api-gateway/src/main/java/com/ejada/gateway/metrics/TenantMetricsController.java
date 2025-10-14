package com.ejada.gateway.metrics;

import com.ejada.common.dto.BaseResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Exposes tenant level request counts so platform operators can quickly assess
 * traffic distribution across the SaaS estate.
 */
@RestController
@RequestMapping("/api/v1/metrics")
public class TenantMetricsController {

  private final TenantRequestMetricsTracker metricsTracker;

  public TenantMetricsController(TenantRequestMetricsTracker metricsTracker) {
    this.metricsTracker = metricsTracker;
  }

  @GetMapping("/tenants")
  public Mono<BaseResponse<List<TenantRequestMetric>>> tenantMetrics() {
    return Mono.fromSupplier(metricsTracker::snapshot)
        .map(data -> BaseResponse.success("Tenant request counts for the last 24 hours", data));
  }
}
