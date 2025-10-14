package com.ejada.gateway.security.cors;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpHeaders;

/**
 * Lightweight in-memory cache for CORS preflight responses. The cache reduces
 * repeated processing of identical preflight requests by storing the computed
 * response headers for a configurable period.
 */
public class CorsPreflightCache {

  private final Duration ttl;
  private final Map<String, CacheEntry> entries = new ConcurrentHashMap<>();

  public CorsPreflightCache(Duration ttl) {
    if (ttl == null || ttl.isNegative() || ttl.isZero()) {
      this.ttl = Duration.ofMinutes(10);
    } else {
      this.ttl = ttl;
    }
  }

  public Optional<HttpHeaders> lookup(String key) {
    if (key == null) {
      return Optional.empty();
    }
    CacheEntry entry = entries.get(key);
    if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
      entries.remove(key);
      return Optional.empty();
    }
    return Optional.of(copy(entry.headers()));
  }

  public void store(String key, HttpHeaders headers) {
    if (key == null || headers == null) {
      return;
    }
    entries.put(key, new CacheEntry(copy(headers), Instant.now().plus(ttl)));
  }

  private HttpHeaders copy(HttpHeaders headers) {
    HttpHeaders clone = new HttpHeaders();
    headers.forEach((name, values) -> clone.put(name, values != null ? List.copyOf(values) : null));
    return clone;
  }

  private record CacheEntry(HttpHeaders headers, Instant expiresAt) { }
}
