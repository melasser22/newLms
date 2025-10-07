package com.ejada.gateway.metrics;

import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.util.StringUtils;

/**
 * Centralised gateway metrics for transformation and caching concerns.
 */
public class GatewayMetrics {

  private final Counter requestTransformations;

  private final Counter responseTransformations;

  private final AtomicLong cacheHits = new AtomicLong();

  private final AtomicLong cacheMisses = new AtomicLong();

  private final MeterRegistry meterRegistry;

  public GatewayMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.requestTransformations = Counter.builder("gateway.transformation.applied")
        .tag("phase", "request")
        .description("Number of request transformations applied by the gateway")
        .register(meterRegistry);

    this.responseTransformations = Counter.builder("gateway.transformation.applied")
        .tag("phase", "response")
        .description("Number of response transformations applied by the gateway")
        .register(meterRegistry);

    Gauge.builder("gateway.cache.hit_rate", this, GatewayMetrics::calculateHitRate)
        .description("Cache hit ratio for gateway response caching")
        .register(meterRegistry);
  }

  public void recordRequestTransformation() {
    requestTransformations.increment();
  }

  public void recordResponseTransformation() {
    responseTransformations.increment();
  }

  public void recordCacheHit(String routeId, String cacheKey, CacheState state) {
    cacheHits.incrementAndGet();
    meterRegistry.counter("gateway.cache.events",
        "routeId", sanitize(routeId),
        "cacheKey", sanitize(cacheKey),
        "event", eventName(state)).increment();
  }

  public void recordCacheMiss(String routeId, String cacheKey) {
    cacheMisses.incrementAndGet();
    meterRegistry.counter("gateway.cache.events",
        "routeId", sanitize(routeId),
        "cacheKey", sanitize(cacheKey),
        "event", "miss").increment();
  }

  double calculateHitRate() {
    long hits = cacheHits.get();
    long misses = cacheMisses.get();
    long total = hits + misses;
    if (total == 0) {
      return 0.0d;
    }
    return (double) hits / total;
  }

  private String sanitize(String value) {
    if (!StringUtils.hasText(value)) {
      return "unknown";
    }
    return value;
  }

  private String eventName(CacheState state) {
    if (state == null) {
      return "hit";
    }
    return switch (state) {
      case STALE -> "stale";
      case NOT_MODIFIED -> "not-modified";
      default -> "hit";
    };
  }

  public enum CacheState {
    FRESH,
    STALE,
    NOT_MODIFIED
  }
}

