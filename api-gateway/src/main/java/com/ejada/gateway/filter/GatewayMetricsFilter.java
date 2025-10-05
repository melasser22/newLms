package com.ejada.gateway.filter;

import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.observability.GatewayTracingHelper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Central metrics filter that records request volumes and latency measurements.
 */
public class GatewayMetricsFilter implements WebFilter, Ordered {

  private final MeterRegistry meterRegistry;
  private final GatewayTracingHelper tracingHelper;

  public GatewayMetricsFilter(MeterRegistry meterRegistry, GatewayTracingHelper tracingHelper) {
    this.meterRegistry = meterRegistry;
    this.tracingHelper = tracingHelper;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 20;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    Timer.Sample sample = Timer.start(meterRegistry);
    long start = System.nanoTime();
    return chain.filter(exchange)
        .doFinally(signalType -> {
          long duration = System.nanoTime() - start;
          recordMetrics(exchange, sample, duration);
          tracingHelper.tagExchange(exchange);
        });
  }

  private void recordMetrics(ServerWebExchange exchange, Timer.Sample sample, long durationNanos) {
    HttpStatusCode status = exchange.getResponse().getStatusCode();
    int statusCode = status != null ? status.value() : HttpStatus.OK.value();
    String tenant = trimToDefault(exchange.getAttribute(GatewayRequestAttributes.TENANT_ID));
    meterRegistry.counter("gateway.requests.by_tenant",
            "tenantId", tenant,
            "statusCode", String.valueOf(statusCode))
        .increment();

    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    String routeId = route != null ? trimToDefault(route.getId()) : "unknown";
    Timer timer = Timer.builder("gateway.route.latency")
        .description("Latency of requests routed through the gateway")
        .publishPercentileHistogram()
        .publishPercentiles(0.5, 0.95, 0.99)
        .tags("routeId", routeId)
        .register(meterRegistry);
    sample.stop(timer);

    meterRegistry.timer("gateway.request.duration", "tenantId", tenant, "routeId", routeId)
        .record(Duration.ofNanos(durationNanos));
  }

  private String trimToDefault(String value) {
    if (!StringUtils.hasText(value)) {
      return "unknown";
    }
    return value.trim();
  }
}
