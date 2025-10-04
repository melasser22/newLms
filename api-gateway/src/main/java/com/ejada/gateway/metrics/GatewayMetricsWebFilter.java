package com.ejada.gateway.metrics;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive web filter that records request/response metrics for every gateway exchange.
 */
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class GatewayMetricsWebFilter implements WebFilter {

  private final GatewayMetrics gatewayMetrics;

  public GatewayMetricsWebFilter(GatewayMetrics gatewayMetrics) {
    this.gatewayMetrics = gatewayMetrics;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    long start = System.nanoTime();
    return chain.filter(exchange)
        .doFinally(signalType -> gatewayMetrics.recordExchange(exchange, System.nanoTime() - start));
  }
}
