package com.ejada.gateway.loadbalancer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.StringUtils;

/**
 * Maintains rolling insight into the health of discovered service instances so custom load
 * balancing strategies can make informed decisions. The aggregator consumes metadata published by
 * service discovery, blends it with locally observed latency samples and produces an effective
 * weight for each instance.
 */
public class LoadBalancerHealthCheckAggregator {

  private static final double DEFAULT_HEALTH_SCORE = 1.0d;
  private static final double MIN_WEIGHT = 0.0d;
  private static final double MAX_WEIGHT = 1.0d;
  private static final double DEFAULT_BASELINE_RESPONSE_TIME_MS = 250d;

  private final ConcurrentMap<String, InstanceState> stateByInstance = new ConcurrentHashMap<>();
  private final double baselineResponseTimeMs;
  private final Clock clock;

  public LoadBalancerHealthCheckAggregator() {
    this(Duration.ofMillis((long) DEFAULT_BASELINE_RESPONSE_TIME_MS), Clock.systemUTC());
  }

  public LoadBalancerHealthCheckAggregator(Duration baselineResponseTime) {
    this(baselineResponseTime, Clock.systemUTC());
  }

  public LoadBalancerHealthCheckAggregator(Duration baselineResponseTime, Clock clock) {
    this.baselineResponseTimeMs = Math.max(1d,
        baselineResponseTime != null ? baselineResponseTime.toMillis() : DEFAULT_BASELINE_RESPONSE_TIME_MS);
    this.clock = (clock != null) ? clock : Clock.systemUTC();
  }

  /** Updates or creates the aggregated state for the supplied instance. */
  public InstanceState update(ServiceInstance instance) {
    Objects.requireNonNull(instance, "instance");
    return stateByInstance.compute(instanceKey(instance), (key, existing) -> merge(instance, existing));
  }

  /** Records an observed response time so future weight calculations can react to live traffic. */
  public void recordResponseTime(ServiceInstance instance, Duration responseTime) {
    if (instance == null || responseTime == null) {
      return;
    }
    double latency = Math.max(1d, responseTime.toMillis());
    stateByInstance.compute(instanceKey(instance), (key, existing) -> {
      InstanceState base = (existing != null) ? existing : merge(instance, null);
      double measured = (base.sampleCount > 0)
          ? ((base.measuredResponseTimeMs * base.sampleCount) + latency) / (base.sampleCount + 1)
          : latency;
      int samples = base.sampleCount + 1;
      double effectiveWeight = computeWeight(base.healthScore, measured, base.availability, base.rolloutPhase);
      return base.withMeasurements(measured, samples, effectiveWeight, Instant.now(clock));
    });
  }

  /** Snapshot of all tracked instance states sorted by service and instance id. */
  public List<InstanceState> snapshot() {
    return snapshot(null);
  }

  /** Snapshot filtered by service id (if provided). */
  public List<InstanceState> snapshot(String serviceId) {
    return stateByInstance.values().stream()
        .filter(state -> !StringUtils.hasText(serviceId) || serviceId.equals(state.serviceId))
        .sorted(Comparator.comparing(InstanceState::getServiceId)
            .thenComparing(InstanceState::getInstanceId))
        .map(InstanceState::copy)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private InstanceState merge(ServiceInstance instance, InstanceState existing) {
    Map<String, String> metadata = instance.getMetadata();
    double healthScore = resolveHealthScore(metadata, existing);
    double metadataLatency = resolveResponseTime(metadata, existing);
    double measuredLatency = (existing != null && existing.sampleCount > 0)
        ? existing.measuredResponseTimeMs
        : metadataLatency;
    Availability availability = resolveAvailability(metadata, existing);
    String rollout = resolveRolloutPhase(metadata, existing);
    String zone = resolveZone(metadata, existing);
    Instant now = Instant.now(clock);
    double weight = computeWeight(healthScore, measuredLatency, availability, rollout);
    return new InstanceState(
        instance.getServiceId(),
        resolveInstanceId(instance),
        zone,
        healthScore,
        metadataLatency,
        measuredLatency,
        existing != null ? existing.sampleCount : 0,
        availability,
        rollout,
        weight,
        now);
  }

  private double resolveHealthScore(Map<String, String> metadata, InstanceState existing) {
    double fallback = existing != null ? existing.healthScore : DEFAULT_HEALTH_SCORE;
    return clamp(resolveDouble(metadata, fallback, "healthScore", "health-score", "health_score"));
  }

  private double resolveResponseTime(Map<String, String> metadata, InstanceState existing) {
    double fallback = (existing != null && existing.metadataResponseTimeMs > 0)
        ? existing.metadataResponseTimeMs
        : baselineResponseTimeMs;
    double value = resolveDouble(metadata, fallback,
        "avgResponseTimeMs",
        "avg-response-time",
        "average-response-time",
        "response-time-ms");
    return (value > 0) ? value : fallback;
  }

  private Availability resolveAvailability(Map<String, String> metadata, InstanceState existing) {
    Availability fallback = existing != null ? existing.availability : Availability.UP;
    String raw = firstNonBlank(metadata,
        "availability",
        "status",
        "state",
        "healthStatus");
    if (!StringUtils.hasText(raw)) {
      return fallback;
    }
    String candidate = raw.trim().toUpperCase(Locale.ROOT);
    if ("OUT_OF_SERVICE".equals(candidate) || "DRAINING".equals(candidate)) {
      return Availability.DEGRADED;
    }
    try {
      return Availability.valueOf(candidate);
    } catch (IllegalArgumentException ex) {
      return fallback;
    }
  }

  private String resolveRolloutPhase(Map<String, String> metadata, InstanceState existing) {
    String fallback = existing != null ? existing.rolloutPhase : "steady";
    String value = firstNonBlank(metadata,
        "rolloutPhase",
        "rollout-phase",
        "deploymentPhase",
        "deployment-phase",
        "deployment");
    if (!StringUtils.hasText(value)) {
      return fallback;
    }
    return value.trim();
  }

  private String resolveZone(Map<String, String> metadata, InstanceState existing) {
    String fallback = existing != null ? existing.zone : "unknown";
    String value = firstNonBlank(metadata,
        "zone",
        "zoneId",
        "zone-id",
        "cloud.zone",
        "spring-cloud-loadbalancer-zone");
    if (!StringUtils.hasText(value)) {
      return fallback;
    }
    return value.trim();
  }

  private double resolveDouble(Map<String, String> metadata, double fallback, String... keys) {
    for (String key : keys) {
      if (!metadata.containsKey(key)) {
        continue;
      }
      String raw = metadata.get(key);
      if (!StringUtils.hasText(raw)) {
        continue;
      }
      try {
        return Double.parseDouble(raw.trim());
      } catch (NumberFormatException ignored) {
        // ignore malformed values and continue
      }
    }
    return fallback;
  }

  private String firstNonBlank(Map<String, String> metadata, String... keys) {
    for (String key : keys) {
      String value = metadata.get(key);
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }

  private double computeWeight(double healthScore, double responseTimeMs, Availability availability,
      String rolloutPhase) {
    double health = clamp(healthScore);
    double latency = (responseTimeMs > 0) ? responseTimeMs : baselineResponseTimeMs;
    double latencyScore = baselineResponseTimeMs / (baselineResponseTimeMs + latency);
    latencyScore = clamp(latencyScore);
    double weight = health * latencyScore;
    if (availability == Availability.DEGRADED) {
      weight *= 0.5d;
    } else if (availability == Availability.DOWN) {
      return 0d;
    }
    if (StringUtils.hasText(rolloutPhase)) {
      String normalized = rolloutPhase.trim().toLowerCase(Locale.ROOT);
      if (normalized.contains("warm") || normalized.contains("canary")) {
        weight *= 0.6d;
      } else if (normalized.contains("drain")) {
        weight *= 0.2d;
      }
    }
    return clamp(weight);
  }

  private static double clamp(double value) {
    if (Double.isNaN(value)) {
      return MIN_WEIGHT;
    }
    if (value < MIN_WEIGHT) {
      return MIN_WEIGHT;
    }
    if (value > MAX_WEIGHT) {
      return MAX_WEIGHT;
    }
    return value;
  }

  private String instanceKey(ServiceInstance instance) {
    return instance.getServiceId() + '@' + resolveInstanceId(instance);
  }

  public static String resolveInstanceId(ServiceInstance instance) {
    if (instance == null) {
      return "unknown";
    }
    String instanceId = instance.getInstanceId();
    if (StringUtils.hasText(instanceId)) {
      return instanceId;
    }
    String host = instance.getHost();
    int port = instance.getPort();
    return host + ':' + port;
  }

  /** Canonical instance availability derived from discovery metadata. */
  public enum Availability {
    UP,
    DEGRADED,
    DOWN
  }

  /** Immutable snapshot of an instance's computed health state. */
  public static final class InstanceState {

    private final String serviceId;
    private final String instanceId;
    private final String zone;
    private final double healthScore;
    private final double metadataResponseTimeMs;
    private final double measuredResponseTimeMs;
    private final int sampleCount;
    private final Availability availability;
    private final String rolloutPhase;
    private final double effectiveWeight;
    private final Instant updatedAt;

    private InstanceState(String serviceId,
        String instanceId,
        String zone,
        double healthScore,
        double metadataResponseTimeMs,
        double measuredResponseTimeMs,
        int sampleCount,
        Availability availability,
        String rolloutPhase,
        double effectiveWeight,
        Instant updatedAt) {
      this.serviceId = serviceId;
      this.instanceId = instanceId;
      this.zone = zone;
      this.healthScore = healthScore;
      this.metadataResponseTimeMs = metadataResponseTimeMs;
      this.measuredResponseTimeMs = measuredResponseTimeMs;
      this.sampleCount = sampleCount;
      this.availability = availability;
      this.rolloutPhase = rolloutPhase;
      this.effectiveWeight = effectiveWeight;
      this.updatedAt = updatedAt;
    }

    private InstanceState withMeasurements(double measuredResponseTimeMs, int sampleCount,
        double effectiveWeight, Instant updatedAt) {
      return new InstanceState(serviceId, instanceId, zone, healthScore, metadataResponseTimeMs,
          measuredResponseTimeMs, sampleCount, availability, rolloutPhase, effectiveWeight, updatedAt);
    }

    private InstanceState copy() {
      return new InstanceState(serviceId, instanceId, zone, healthScore, metadataResponseTimeMs,
          measuredResponseTimeMs, sampleCount, availability, rolloutPhase, effectiveWeight, updatedAt);
    }

    public String getServiceId() {
      return serviceId;
    }

    public String getInstanceId() {
      return instanceId;
    }

    public String getZone() {
      return zone;
    }

    public double getHealthScore() {
      return healthScore;
    }

    public double getMetadataResponseTimeMs() {
      return metadataResponseTimeMs;
    }

    public double getMeasuredResponseTimeMs() {
      return measuredResponseTimeMs;
    }

    public int getSampleCount() {
      return sampleCount;
    }

    public Availability getAvailability() {
      return availability;
    }

    public String getRolloutPhase() {
      return rolloutPhase;
    }

    public double getEffectiveWeight() {
      return effectiveWeight;
    }

    public Instant getUpdatedAt() {
      return updatedAt;
    }

    public double getAverageResponseTimeMs() {
      if (sampleCount > 0 && measuredResponseTimeMs > 0) {
        return measuredResponseTimeMs;
      }
      if (metadataResponseTimeMs > 0) {
        return metadataResponseTimeMs;
      }
      return measuredResponseTimeMs > 0 ? measuredResponseTimeMs : DEFAULT_BASELINE_RESPONSE_TIME_MS;
    }

    @Override
    public String toString() {
      return "InstanceState{"
          + "serviceId='" + serviceId + '\''
          + ", instanceId='" + instanceId + '\''
          + ", zone='" + zone + '\''
          + ", healthScore=" + healthScore
          + ", averageResponseTimeMs=" + getAverageResponseTimeMs()
          + ", sampleCount=" + sampleCount
          + ", availability=" + availability
          + ", rolloutPhase='" + rolloutPhase + '\''
          + ", effectiveWeight=" + effectiveWeight
          + ", updatedAt=" + updatedAt
          + '}';
    }
  }
}
