package com.ejada.gateway.subscription;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * Snapshot of a tenant subscription returned by the subscription service and cached in Redis.
 */
public record SubscriptionRecord(
    boolean active,
    Set<String> features,
    Map<String, FeatureAllocation> allocations,
    Instant expiresAt,
    String status,
    Instant fetchedAt,
    String upgradeUrl) {

  public SubscriptionRecord {
    features = (features == null) ? Set.of() : Set.copyOf(features);
    allocations = normaliseAllocations(allocations, features);
    status = StringUtils.hasText(status) ? status : (active ? "ACTIVE" : "INACTIVE");
    fetchedAt = (fetchedAt == null) ? Instant.now() : fetchedAt;
  }

  private static Map<String, FeatureAllocation> normaliseAllocations(
      Map<String, FeatureAllocation> allocations, Set<String> features) {
    Map<String, FeatureAllocation> map = new LinkedHashMap<>();
    if (features != null) {
      for (String feature : features) {
        if (StringUtils.hasText(feature)) {
          map.put(featureKey(feature), new FeatureAllocation(feature, true, null));
        }
      }
    }
    if (allocations != null) {
      allocations.forEach((name, allocation) -> {
        if (!StringUtils.hasText(name)) {
          return;
        }
        FeatureAllocation value = (allocation == null)
            ? new FeatureAllocation(name, true, null)
            : allocation.withFeature(name);
        map.put(featureKey(name), value);
      });
    }
    return Collections.unmodifiableMap(map);
  }

  private static String featureKey(String feature) {
    return feature.toLowerCase(Locale.ROOT);
  }

  public static SubscriptionRecord of(boolean active, Set<String> features, Instant expiresAt,
      Map<String, FeatureAllocation> allocations, String upgradeUrl) {
    return new SubscriptionRecord(active, features, allocations, expiresAt,
        active ? "ACTIVE" : "INACTIVE", Instant.now(), upgradeUrl);
  }

  public static SubscriptionRecord inactive() {
    return new SubscriptionRecord(false, Set.of(), Map.of(), null, "INACTIVE", Instant.now(), null);
  }

  public boolean isActive() {
    return active && !isExpired();
  }

  public boolean isExpired() {
    return expiresAt != null && expiresAt.isBefore(Instant.now());
  }

  public boolean isWithinGrace(Duration gracePeriod) {
    if (expiresAt == null || gracePeriod == null || gracePeriod.isZero() || gracePeriod.isNegative()) {
      return false;
    }
    Instant graceEnd = expiresAt.plus(gracePeriod);
    Instant now = Instant.now();
    return now.isAfter(expiresAt) && !now.isAfter(graceEnd);
  }

  public Duration graceRemaining(Duration gracePeriod) {
    if (!isWithinGrace(gracePeriod)) {
      return Duration.ZERO;
    }
    Instant graceEnd = expiresAt.plus(gracePeriod);
    return Duration.between(Instant.now(), graceEnd);
  }

  public boolean hasFeature(String feature) {
    if (!StringUtils.hasText(feature)) {
      return true;
    }
    return Optional.ofNullable(allocations.get(featureKey(feature)))
        .map(FeatureAllocation::enabled)
        .orElse(false);
  }

  @JsonIgnore
  public Optional<FeatureAllocation> allocationFor(String feature) {
    if (!StringUtils.hasText(feature)) {
      return Optional.empty();
    }
    return Optional.ofNullable(allocations.get(featureKey(feature)));
  }

  @JsonIgnore
  public Set<String> enabledFeatures() {
    return allocations.values().stream()
        .filter(FeatureAllocation::enabled)
        .map(FeatureAllocation::feature)
        .collect(Collectors.toUnmodifiableSet());
  }

  public record FeatureAllocation(String feature, boolean enabled, Long limit) {

    public FeatureAllocation {
      feature = Objects.requireNonNullElse(feature, "");
    }

    private FeatureAllocation withFeature(String featureName) {
      return new FeatureAllocation(featureName, enabled, limit);
    }

    @JsonIgnore
    public boolean unlimited() {
      return limit == null || limit < 0;
    }
  }
}
