package com.ejada.gateway.fallback;

import java.time.Instant;
import java.util.Set;

/**
 * Snapshot of cached catalogue tier data returned when the downstream
 * catalogue service is unavailable. Exposes the tenant's effective tier and
 * enabled features that were last synchronised with the subscription service.
 */
public record CatalogFallbackPayload(
    String tier,
    Set<String> features,
    Instant cachedAt) {

  public CatalogFallbackPayload {
    tier = (tier == null || tier.isBlank()) ? "unknown" : tier.trim();
    features = (features == null) ? Set.of() : Set.copyOf(features);
    cachedAt = (cachedAt == null) ? Instant.now() : cachedAt;
  }
}
