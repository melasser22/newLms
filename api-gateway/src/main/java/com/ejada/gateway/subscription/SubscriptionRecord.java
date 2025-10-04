package com.ejada.gateway.subscription;

import java.time.Instant;
import java.util.Set;

/**
 * Simplified snapshot of a tenant subscription used for cache warmup and enforcement.
 */
public record SubscriptionRecord(boolean active, Set<String> features, Instant expiresAt, String status) {

  public SubscriptionRecord {
    features = (features != null) ? Set.copyOf(features) : Set.of();
    status = (status != null) ? status : (active ? "ACTIVE" : "INACTIVE");
  }

  public static SubscriptionRecord of(boolean active, Set<String> features, Instant expiresAt) {
    return new SubscriptionRecord(active, features, expiresAt, active ? "ACTIVE" : "INACTIVE");
  }

  public static SubscriptionRecord inactive() {
    return new SubscriptionRecord(false, Set.of(), null, "INACTIVE");
  }

  public boolean isActive() {
    if (!active) {
      return false;
    }
    return expiresAt == null || expiresAt.isAfter(Instant.now());
  }

  public boolean hasFeature(String feature) {
    if (feature == null || feature.isBlank()) {
      return true;
    }
    return features.stream().anyMatch(value -> value.equalsIgnoreCase(feature));
  }
}
