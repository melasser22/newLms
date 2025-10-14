package com.ejada.gateway.versioning.preference;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.versioning.VersionNumber;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
public class TenantVersionPreferenceService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TenantVersionPreferenceService.class);

  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  private final TenantApiVersionPreferenceRepository repository;

  private final ConcurrentMap<CacheKey, CachedPreference> cache = new ConcurrentHashMap<>();

  public TenantVersionPreferenceService(TenantApiVersionPreferenceRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  public Mono<Optional<String>> resolvePreferredVersion(ServerWebExchange exchange) {
    String tenantId = resolveTenant(exchange);
    if (!StringUtils.hasText(tenantId)) {
      return Mono.just(Optional.empty());
    }
    String resource = normaliseResource(exchange.getRequest().getPath().value());
    if (!StringUtils.hasText(resource)) {
      return Mono.just(Optional.empty());
    }

    CacheKey key = new CacheKey(tenantId, resource);
    CachedPreference cached = cache.get(key);
    if (cached != null && !cached.isExpired()) {
      return Mono.just(Optional.ofNullable(cached.version()));
    }

    return repository.findFirstByTenantIdIgnoreCaseAndResource(tenantId, resource)
        .map(entity -> VersionNumber.canonicaliseOrNull(entity.getPreferredVersion()))
        .defaultIfEmpty(null)
        .map(optionalVersion -> {
          cache.put(key, new CachedPreference(optionalVersion, Instant.now().plus(CACHE_TTL)));
          if (optionalVersion != null) {
            LOGGER.debug("Resolved tenant {} preference {} for resource {}", tenantId, optionalVersion,
                resource);
          }
          return Optional.ofNullable(optionalVersion);
        })
        .onErrorResume(ex -> {
          LOGGER.warn("Failed to resolve tenant version preference for {} {}", tenantId, resource, ex);
          return Mono.just(Optional.empty());
        });
  }

  private String resolveTenant(ServerWebExchange exchange) {
    Object attribute = exchange.getAttribute(GatewayRequestAttributes.TENANT_ID);
    if (attribute instanceof String attributeTenant && StringUtils.hasText(attributeTenant)) {
      return attributeTenant;
    }
    ServerHttpRequest request = exchange.getRequest();
    String headerTenant = request.getHeaders().getFirst(HeaderNames.X_TENANT_ID);
    if (StringUtils.hasText(headerTenant)) {
      return headerTenant.trim();
    }
    return null;
  }

  private String normaliseResource(String rawPath) {
    if (!StringUtils.hasText(rawPath)) {
      return null;
    }
    String[] segments = rawPath.split("/");
    List<String> cleaned = new ArrayList<>();
    for (String segment : segments) {
      if (!StringUtils.hasText(segment)) {
        continue;
      }
      String trimmed = segment.trim();
      String lower = trimmed.toLowerCase(Locale.ROOT);
      if (lower.matches("v\\d+(?:\\.\\d+)*")) {
        continue;
      }
      cleaned.add(lower);
    }
    if (cleaned.isEmpty()) {
      return null;
    }
    int limit = Math.min(2, cleaned.size());
    return '/' + String.join("/", cleaned.subList(0, limit));
  }

  private record CacheKey(String tenantId, String resource) {
  }

  private record CachedPreference(String version, Instant expiresAt) {

    boolean isExpired() {
      return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
  }
}
