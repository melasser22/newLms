package com.ejada.gateway.metrics;

import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Centralised gateway metrics for transformation and caching concerns.
 */
public class GatewayMetrics {

  private final Counter requestTransformations;

  private final Counter responseTransformations;

  private final AtomicLong cacheHits = new AtomicLong();

  private final AtomicLong cacheMisses = new AtomicLong();

  public GatewayMetrics(MeterRegistry meterRegistry) {
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

  public void recordCacheHit() {
    cacheHits.incrementAndGet();
  }

  public void recordCacheMiss() {
    cacheMisses.incrementAndGet();
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
}

