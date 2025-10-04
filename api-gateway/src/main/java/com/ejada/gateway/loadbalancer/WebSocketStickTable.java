package com.ejada.gateway.loadbalancer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.StringUtils;

/**
 * Maintains sticky associations for WebSocket handshakes so upgraded connections continue to use
 * the same downstream instance. Entries expire after a configurable TTL to avoid leaking memory.
 */
public class WebSocketStickTable {

  private final ConcurrentMap<String, StickRecord> records = new ConcurrentHashMap<>();
  private final Duration ttl;
  private final Clock clock;

  public WebSocketStickTable(Duration ttl) {
    this(ttl, Clock.systemUTC());
  }

  public WebSocketStickTable(Duration ttl, Clock clock) {
    this.ttl = (ttl == null || ttl.isNegative() || ttl.isZero()) ? Duration.ofMinutes(10) : ttl;
    this.clock = (clock != null) ? clock : Clock.systemUTC();
  }

  public Optional<ServiceInstance> lookup(String key, String serviceId, List<ServiceInstance> candidates) {
    if (!StringUtils.hasText(key)) {
      return Optional.empty();
    }
    StickRecord record = records.get(key);
    if (record == null) {
      return Optional.empty();
    }
    if (record.expiresAt().isBefore(Instant.now(clock))) {
      records.remove(key, record);
      return Optional.empty();
    }
    return candidates.stream()
        .filter(instance -> serviceId.equals(instance.getServiceId()))
        .filter(instance -> record.instanceId().equals(LoadBalancerHealthCheckAggregator.resolveInstanceId(instance)))
        .findFirst();
  }

  public void record(String key, ServiceInstance instance) {
    if (!StringUtils.hasText(key) || instance == null) {
      return;
    }
    StickRecord record = new StickRecord(instance.getServiceId(),
        LoadBalancerHealthCheckAggregator.resolveInstanceId(instance),
        Instant.now(clock).plus(ttl));
    records.put(key, record);
  }

  public Map<String, StickRecord> snapshot() {
    return Map.copyOf(records);
  }

  public record StickRecord(String serviceId, String instanceId, Instant expiresAt) {
  }
}
