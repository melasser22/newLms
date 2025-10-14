package com.ejada.gateway.admin;

import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.admin.model.AdminOverview;
import com.ejada.gateway.admin.model.AdminRouteView;
import com.ejada.gateway.admin.model.DetailedHealthStatus;
import com.ejada.gateway.loadbalancer.LoadBalancerHealthCheckAggregator;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST entrypoint that exposes aggregated operational views across downstream services and
 * configured gateway routes.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminAggregationController {

  private final AdminAggregationService aggregationService;
  private final AdminTenantAggregationService tenantAggregationService;

  public AdminAggregationController(AdminAggregationService aggregationService,
      AdminTenantAggregationService tenantAggregationService) {
    this.aggregationService = aggregationService;
    this.tenantAggregationService = tenantAggregationService;
  }

  @GetMapping("/overview")
  public Mono<BaseResponse<AdminOverview>> overview() {
    return aggregationService.fetchOverview()
        .map(data -> BaseResponse.success("Aggregated gateway overview", data));
  }

  @GetMapping("/routes")
  public Mono<BaseResponse<List<AdminRouteView>>> routes() {
    return Mono.fromSupplier(aggregationService::describeRoutes)
        .map(data -> BaseResponse.success("Gateway route catalogue", data));
  }

  @GetMapping("/health/detailed")
  public Mono<BaseResponse<DetailedHealthStatus>> detailedHealth() {
    return aggregationService.fetchDetailedHealth()
        .map(data -> BaseResponse.success("Gateway dependency health", data));
  }

  @GetMapping("/loadbalancer/health")
  public Mono<BaseResponse<List<LoadBalancerHealthCheckAggregator.InstanceState>>> loadBalancerHealth(
      @RequestParam(name = "serviceId", required = false) String serviceId) {
    return Mono.fromSupplier(() -> aggregationService.collectLoadBalancerHealth(serviceId))
        .map(data -> BaseResponse.success("Load balancer health", data));
  }

  @GetMapping("/tenants/{tenantId}/aggregate")
  public Mono<BaseResponse<AdminTenantAggregateResponse>> tenantAggregate(@PathVariable Integer tenantId) {
    return tenantAggregationService.aggregate(tenantId)
        .map(data -> BaseResponse.success("Tenant aggregated overview", data));
  }
}
