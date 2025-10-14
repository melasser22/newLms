package com.ejada.gateway.observability;

import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.admin.AdminAggregationService;
import com.ejada.gateway.admin.model.DetailedHealthStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Aggregated health endpoint that reports the status of core dependencies in a
 * single call for operational dashboards.
 */
@RestController
@RequestMapping("/api/v1/health")
public class GatewayHealthController {

  private final AdminAggregationService aggregationService;

  public GatewayHealthController(AdminAggregationService aggregationService) {
    this.aggregationService = aggregationService;
  }

  @GetMapping("/aggregate")
  public Mono<BaseResponse<DetailedHealthStatus>> aggregateHealth() {
    return aggregationService.fetchDetailedHealth()
        .map(data -> BaseResponse.success("Gateway dependency health", data));
  }
}
