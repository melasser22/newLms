package com.ejada.gateway.metrics;

import com.ejada.gateway.context.GatewayRequestAttributes;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * Centralised facade for publishing custom gateway metrics via Micrometer.
 */
public class GatewayMetrics {

  private final MeterRegistry meterRegistry;
  private final AtomicLong subscriptionCacheHits = new AtomicLong();
  private final AtomicLong subscriptionCacheMisses = new AtomicLong();

  public GatewayMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    Gauge.builder("gateway.subscription.validation.cache_hit_rate", this, GatewayMetrics::cacheHitRate)
        .description("Ratio of subscription validations served from cache")
        .strongReference(true)
        .register(meterRegistry);
  }

  public void recordExchange(ServerWebExchange exchange, long durationNanos) {
    String tenantId = valueOrDefault(exchange.getAttribute(GatewayRequestAttributes.TENANT_ID), "anonymous");
    HttpStatus status = exchange.getResponse().getStatusCode();
    Integer rawStatus = exchange.getResponse().getRawStatusCode();
    String statusCode;
    if (status != null) {
      statusCode = String.valueOf(status.value());
    } else if (rawStatus != null) {
      statusCode = String.valueOf(rawStatus);
    } else {
      statusCode = "200";
    }
    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    String routeId = (route != null && StringUtils.hasText(route.getId())) ? route.getId() : "unknown";

    meterRegistry.counter("gateway.requests.by_tenant", Tags.of("tenantId", tenantId, "statusCode", statusCode)).increment();

    Timer.builder("gateway.route.latency")
        .description("Latency distribution per gateway route")
        .publishPercentileHistogram()
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .tags("routeId", routeId)
        .register(meterRegistry)
        .record(durationNanos, TimeUnit.NANOSECONDS);
  }

  public void recordRateLimitRejection(String strategy, String tenantId) {
    String resolvedStrategy = StringUtils.hasText(strategy) ? strategy : "unknown";
    String resolvedTenant = StringUtils.hasText(tenantId) ? tenantId : "anonymous";
    meterRegistry.counter("gateway.ratelimit.rejections", Tags.of("strategy", resolvedStrategy, "tenantId", resolvedTenant)).increment();
  }

  public void recordSubscriptionCacheHit() {
    subscriptionCacheHits.incrementAndGet();
  }

  public void recordSubscriptionCacheMiss() {
    subscriptionCacheMisses.incrementAndGet();
  }

  public void recordSubscriptionValidationOutcome(String tenantId, boolean success) {
    String resolvedTenant = valueOrDefault(tenantId, "anonymous");
    meterRegistry.counter("gateway.subscription.validation.requests", Tags.of("tenantId", resolvedTenant)).increment();
    if (!success) {
      meterRegistry.counter("gateway.subscription.validation.failures", Tags.of("tenantId", resolvedTenant)).increment();
    }
  }

  private double cacheHitRate() {
    long hits = subscriptionCacheHits.get();
    long misses = subscriptionCacheMisses.get();
    long total = hits + misses;
    if (total == 0) {
      return 1.0d;
    }
    return (double) hits / total;
  }

  private String valueOrDefault(String value, String defaultValue) {
    return StringUtils.hasText(value) ? value : defaultValue;
  }
}
