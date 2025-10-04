package com.ejada.gateway.dr;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Propagates the active region downstream and records failures to trigger failover.
 */
@Component
public class RegionFailoverFilter implements GlobalFilter, Ordered {

  public static final String ACTIVE_REGION_HEADER = "X-Active-Region";

  private final RegionFailoverService failoverService;
  public RegionFailoverFilter(RegionFailoverService failoverService) {
    this.failoverService = failoverService;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!failoverService.isEnabled()) {
      return chain.filter(exchange);
    }
    String region = failoverService.currentRegion();
    ServerHttpRequest request = exchange.getRequest().mutate()
        .header(ACTIVE_REGION_HEADER, region)
        .build();
    return chain.filter(exchange.mutate().request(request).build())
        .doOnSuccess(unused -> handleSuccess(exchange))
        .doOnError(failoverService::recordFailure);
  }

  private void handleSuccess(ServerWebExchange exchange) {
    HttpStatus status = exchange.getResponse().getStatusCode();
    if (status != null && status.is5xxServerError()) {
      failoverService.recordFailure(new IllegalStateException("upstream-5xx"));
    } else {
      failoverService.resetFailures();
    }
  }

  @Override
  public int getOrder() {
    return -50;
  }
}
