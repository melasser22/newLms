package com.ejada.gateway.fallback;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Snapshot of a cached response returned when the gateway serves a Redis backed fallback.
 */
public record CachedFallbackPayload(
    int status,
    Map<String, List<String>> headers,
    String body,
    Instant cachedAt,
    Instant expiresAt,
    Instant staleAt) {

  public CachedFallbackPayload {
    headers = (headers == null) ? Map.of() : Collections.unmodifiableMap(headers.entrySet().stream()
        .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue()))));
    body = body == null ? "" : body;
  }
}
