package com.ejada.shared_starter_ratelimit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Records Micrometer metrics for rate limiter activity.
 */
public class RateLimitMetricsRecorder {

  private static final String REQUEST_METRIC = "ratelimit.requests";
  private static final String REJECTION_METRIC = "ratelimit.rejections";
  private static final String BYPASS_METRIC = "ratelimit.bypass";

  private final MeterRegistry registry;
  private final ConcurrentMap<MetricKey, Counter> requestCounters = new ConcurrentHashMap<>();
  private final ConcurrentMap<MetricKey, Counter> rejectionCounters = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Counter> bypassCounters = new ConcurrentHashMap<>();

  public RateLimitMetricsRecorder(MeterRegistry registry) {
    this.registry = registry;
  }

  public void recordAllowed(String tier, String strategy) {
    if (registry == null) {
      return;
    }
    counter(requestCounters, REQUEST_METRIC, tier, "allowed", strategy).increment();
  }

  public void recordRejected(String tier, String strategy, RateLimitReason reason) {
    if (registry == null) {
      return;
    }
    counter(requestCounters, REQUEST_METRIC, tier, "rejected", strategy).increment();
    counter(rejectionCounters, REJECTION_METRIC, tier, reason.code(), strategy).increment();
  }

  public void recordBypass(RateLimitBypassType type) {
    if (registry == null) {
      return;
    }
    bypassCounters.computeIfAbsent(type.code(), key -> Counter
            .builder(BYPASS_METRIC)
            .tag("type", key)
            .register(registry))
        .increment();
  }

  private Counter counter(ConcurrentMap<MetricKey, Counter> cache, String metric,
      String tier, String result, String strategy) {
    MetricKey key = new MetricKey(metric, tier, result, strategy);
    return cache.computeIfAbsent(key, ignored -> Counter.builder(metric)
        .tag("tier", Objects.toString(tier, "unknown"))
        .tag("result", Objects.toString(result, "unknown"))
        .tag("strategy", Objects.toString(strategy, "unknown"))
        .register(registry));
  }

  private record MetricKey(String metric, String tier, String result, String strategy) {
  }
}
