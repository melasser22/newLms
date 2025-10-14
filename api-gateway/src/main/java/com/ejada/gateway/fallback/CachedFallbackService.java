package com.ejada.gateway.fallback;

import com.ejada.gateway.transformation.ResponseCacheService;
import com.ejada.gateway.transformation.ResponseCacheService.CacheMetadata;
import com.ejada.gateway.transformation.ResponseCacheService.CacheResult;
import com.ejada.gateway.transformation.ResponseCacheService.CachedResponse;
import com.ejada.gateway.metrics.GatewayMetrics.CacheState;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

/**
 * Resolves cached downstream responses from Redis so the gateway can return intelligent fallbacks
 * when a circuit breaker opens.
 */
@Component
public class CachedFallbackService {

  private final ResponseCacheService cacheService;

  public CachedFallbackService(ObjectProvider<ResponseCacheService> cacheServiceProvider) {
    this.cacheService = cacheServiceProvider != null ? cacheServiceProvider.getIfAvailable() : null;
  }

  public Mono<CachedFallbackContext> resolve(String routeId, org.springframework.web.server.ServerWebExchange exchange) {
    if (cacheService == null || !cacheService.isCacheEnabled()) {
      return Mono.empty();
    }
    return cacheService.find(routeId, exchange)
        .filter(CacheResult::hasRoute)
        .filter(result -> !result.isMiss())
        .map(this::toContext);
  }

  private CachedFallbackContext toContext(CacheResult result) {
    CacheMetadata metadata = Objects.requireNonNull(result.metadata(), "metadata");
    CachedResponse response = Objects.requireNonNull(result.response(), "response");
    CacheState cacheState = Optional.ofNullable(result.state()).orElse(CacheState.FRESH);
    Map<String, Object> metadataMap = new LinkedHashMap<>();
    metadataMap.put("cacheKey", metadata.cacheKey());
    metadataMap.put("route", metadata.route() != null ? metadata.route().cacheKeyPrefix() : "unknown");
    metadataMap.put("cacheState", cacheState.name());
    metadataMap.put("cachedAt", response.cachedAt());
    metadataMap.put("expiresAt", response.expiresAt());
    metadataMap.put("staleAt", response.staleAt());
    metadataMap.put("status", response.status());
    Map<String, List<String>> headers = sanitizeHeaders(response.headers());
    CachedFallbackPayload payload = new CachedFallbackPayload(response.status(), headers,
        decodeBody(response.body()), response.cachedAt(), response.expiresAt(), response.staleAt());
    return new CachedFallbackContext(payload, Map.copyOf(metadataMap), cacheState, metadata.cacheKey());
  }

  private String decodeBody(byte[] body) {
    if (body == null || body.length == 0) {
      return "";
    }
    return new String(body, StandardCharsets.UTF_8);
  }

  private Map<String, List<String>> sanitizeHeaders(org.springframework.http.HttpHeaders headers) {
    if (headers == null || headers.isEmpty()) {
      return Map.of();
    }
    Map<String, List<String>> sanitized = new LinkedHashMap<>();
    headers.forEach((key, values) -> {
      if (CollectionUtils.isEmpty(values)) {
        sanitized.put(key, List.of());
      } else {
        sanitized.put(key, List.copyOf(values));
      }
    });
    return Map.copyOf(sanitized);
  }

  public record CachedFallbackContext(
      CachedFallbackPayload payload,
      Map<String, Object> metadata,
      CacheState cacheState,
      String cacheKey) { }
}
