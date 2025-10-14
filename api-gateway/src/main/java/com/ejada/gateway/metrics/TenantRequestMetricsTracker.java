package com.ejada.gateway.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Tracks gateway requests per tenant within a fixed sliding window so the metrics
 * endpoint can report activity over the last 24 hours without querying external
 * storage.
 */
@Component
public class TenantRequestMetricsTracker {

  private static final Duration WINDOW = Duration.ofHours(24);

  private final ConcurrentLinkedQueue<RequestRecord> records = new ConcurrentLinkedQueue<>();
  private final ConcurrentMap<String, LongAdder> counters = new ConcurrentHashMap<>();

  /** Records a completed gateway request for the supplied tenant. */
  public void recordRequest(String tenantId, Instant timestamp) {
    String tenant = normaliseTenant(tenantId);
    Instant eventTime = Objects.requireNonNullElseGet(timestamp, Instant::now);
    records.add(new RequestRecord(tenant, eventTime));
    counters.computeIfAbsent(tenant, key -> new LongAdder()).increment();
    pruneExpired(eventTime.minus(WINDOW));
  }

  /** Returns a snapshot of the request counts per tenant ordered by activity. */
  public List<TenantRequestMetric> snapshot() {
    pruneExpired(Instant.now().minus(WINDOW));
    List<TenantRequestMetric> metrics = new ArrayList<>();
    for (Map.Entry<String, LongAdder> entry : counters.entrySet()) {
      long value = entry.getValue().longValue();
      if (value > 0) {
        metrics.add(new TenantRequestMetric(entry.getKey(), value));
      }
    }
    metrics.sort(Comparator.comparingLong(TenantRequestMetric::requestCount).reversed()
        .thenComparing(TenantRequestMetric::tenantId));
    return metrics;
  }

  private void pruneExpired(Instant threshold) {
    if (threshold == null) {
      return;
    }
    RequestRecord head;
    while ((head = records.peek()) != null && head.timestamp.isBefore(threshold)) {
      records.poll();
      LongAdder adder = counters.get(head.tenant);
      if (adder != null) {
        adder.decrement();
        if (adder.longValue() <= 0) {
          counters.remove(head.tenant, adder);
        }
      }
    }
  }

  private String normaliseTenant(String tenantId) {
    if (!StringUtils.hasText(tenantId)) {
      return "unknown";
    }
    return tenantId.trim();
  }

  private record RequestRecord(String tenant, Instant timestamp) { }
}
