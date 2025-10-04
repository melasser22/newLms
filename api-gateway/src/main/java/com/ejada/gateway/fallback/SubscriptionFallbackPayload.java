package com.ejada.gateway.fallback;

import com.ejada.gateway.subscription.SubscriptionRecord;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Snapshot of cached subscription data served when the subscription service is
 * unavailable.
 */
public record SubscriptionFallbackPayload(
    boolean active,
    String status,
    Instant expiresAt,
    Instant cachedAt,
    Set<String> features,
    Map<String, SubscriptionRecord.FeatureAllocation> allocations) {

  public static SubscriptionFallbackPayload fromRecord(SubscriptionRecord record) {
    if (record == null) {
      return new SubscriptionFallbackPayload(false, "UNKNOWN", null, Instant.now(), Set.of(), Map.of());
    }
    return new SubscriptionFallbackPayload(
        record.isActive(),
        record.status(),
        record.expiresAt(),
        record.fetchedAt(),
        record.enabledFeatures(),
        record.allocations());
  }
}
