package com.ejada.gateway.transformation;

import com.ejada.common.context.ContextManager;
import com.ejada.gateway.config.GatewayCacheProperties;
import com.ejada.gateway.config.GatewayCacheProperties.RouteCacheProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.metrics.GatewayMetrics;
import com.ejada.gateway.metrics.GatewayMetrics.CacheState;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Manages caching of GET responses in Redis.
 */
public class ResponseCacheService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResponseCacheService.class);

  private static final String CACHE_PREFIX = "gateway:cache:";

  private final GatewayCacheProperties properties;

  private final ReactiveStringRedisTemplate redisTemplate;

  private final ObjectMapper objectMapper;

  private final GatewayMetrics metrics;

  private final Map<String, Instant> refreshInFlight = new ConcurrentHashMap<>();

  public ResponseCacheService(GatewayCacheProperties properties,
      ReactiveStringRedisTemplate redisTemplate,
      ObjectMapper objectMapper,
      GatewayMetrics metrics) {
    this.properties = Objects.requireNonNull(properties, "properties");
    this.redisTemplate = redisTemplate;
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    this.metrics = Objects.requireNonNull(metrics, "metrics");
  }

  public boolean isCacheEnabled() {
    return properties.isEnabled() && redisTemplate != null;
  }

  public Optional<RouteCacheProperties> resolveRoute(String routeId, ServerWebExchange exchange) {
    return properties.resolve(routeId, exchange);
  }

  public Optional<CacheMetadata> buildMetadata(String routeId, ServerWebExchange exchange) {
    return resolveRoute(routeId, exchange).flatMap(route -> buildMetadata(route, exchange));
  }

  public Optional<CacheMetadata> buildMetadata(RouteCacheProperties route, ServerWebExchange exchange) {
    if (route == null) {
      return Optional.empty();
    }
    Duration ttl = route.resolvedTtl();
    if (ttl.isZero()) {
      return Optional.empty();
    }
    String tenantId = resolveTenant(exchange, route);
    String canonicalPath = exchange.getRequest().getPath().pathWithinApplication().value();
    String canonicalQuery = canonicalQuery(exchange.getRequest().getQueryParams());
    HttpMethod method = exchange.getRequest().getMethod();
    if (method == null) {
      method = HttpMethod.GET;
    }
    String signatureSource = method.name() + ':' + canonicalPath + '?' + canonicalQuery;
    String signature = DigestUtils.sha1DigestAsHex(signatureSource.getBytes(StandardCharsets.UTF_8));
    String cacheKey = CACHE_PREFIX + route.cacheKeyPrefix() + ':' + tenantId + ':' + signature;
    return Optional.of(new CacheMetadata(route, tenantId, cacheKey, canonicalPath, canonicalQuery, method));
  }

  public Mono<CacheResult> find(String routeId, ServerWebExchange exchange) {
    if (!isCacheEnabled()) {
      return Mono.just(CacheResult.noRoute());
    }
    Optional<CacheMetadata> metadataOptional = buildMetadata(routeId, exchange);
    if (metadataOptional.isEmpty()) {
      return Mono.just(CacheResult.noRoute());
    }
    CacheMetadata metadata = metadataOptional.get();
    String cacheKey = metadata.cacheKey();
    return redisTemplate.opsForValue().get(cacheKey)
        .flatMap(serialized -> deserialize(serialized)
            .map(cached -> Mono.just(evaluateCachedEntry(metadata, cached, exchange)))
            .orElseGet(() -> Mono.just(CacheResult.miss(metadata))))
        .switchIfEmpty(Mono.fromSupplier(() -> CacheResult.miss(metadata)))
        .doOnNext(result -> {
          if (result != null && result.isMiss()) {
            metrics.recordCacheMiss(metadata.route().cacheKeyPrefix(), metadata.canonicalPath());
          }
        });
  }

  public Mono<Void> store(CacheMetadata metadata, CachedResponse response) {
    if (!isCacheEnabled() || metadata == null) {
      return Mono.empty();
    }
    Duration ttl = metadata.route().resolvedTtl();
    if (ttl.isZero()) {
      return Mono.empty();
    }
    Duration staleTtl = metadata.route().resolvedStaleTtl();
    Duration redisTtl = ttl.plus(staleTtl);
    try {
      String serialized = objectMapper.writeValueAsString(response);
      return redisTemplate.opsForValue()
          .set(metadata.cacheKey(), serialized, redisTtl)
          .doOnSuccess(ignored -> LOGGER.debug("Cached response for {}", metadata.cacheKey()))
          .doOnError(ex -> LOGGER.warn("Failed to store cached response for {}", metadata.cacheKey(), ex))
          .then();
    } catch (Exception ex) {
      LOGGER.warn("Failed to serialise cached response", ex);
      return Mono.empty();
    }
  }

  public Mono<Void> invalidate(String routeId, ServerWebExchange exchange) {
    if (!isCacheEnabled()) {
      return Mono.empty();
    }
    Optional<CacheMetadata> metadataOptional = buildMetadata(routeId, exchange);
    if (metadataOptional.isEmpty()) {
      return Mono.empty();
    }
    return invalidate(metadataOptional.get());
  }

  public Mono<Void> invalidate(CacheMetadata metadata) {
    if (!isCacheEnabled() || metadata == null) {
      return Mono.empty();
    }
    return redisTemplate.opsForValue().delete(metadata.cacheKey()).then();
  }

  public Mono<Void> invalidateRoute(RouteCacheProperties route) {
    if (!isCacheEnabled() || route == null) {
      return Mono.empty();
    }
    String pattern = CACHE_PREFIX + route.cacheKeyPrefix() + ":*";
    return invalidateByPattern(pattern);
  }

  public Mono<Void> invalidateRouteForTenant(RouteCacheProperties route, String tenantId) {
    if (!isCacheEnabled() || route == null || !StringUtils.hasText(tenantId)) {
      return Mono.empty();
    }
    String pattern = CACHE_PREFIX + route.cacheKeyPrefix() + ':' + tenantId.toLowerCase(Locale.ROOT) + ":*";
    return invalidateByPattern(pattern);
  }

  public Mono<Void> invalidateByPattern(String pattern) {
    if (!isCacheEnabled() || !StringUtils.hasText(pattern)) {
      return Mono.empty();
    }
    ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();
    return redisTemplate.scan(options)
        .flatMap(key -> redisTemplate.opsForValue().delete(key).thenReturn(key))
        .doOnNext(key -> LOGGER.debug("Invalidated cache entry {}", key))
        .then();
  }

  public CachedResponse snapshotResponse(RouteCacheProperties route,
      ServerHttpResponse response,
      byte[] body,
      String etag,
      Instant capturedAt) {
    HttpHeaders headers = new HttpHeaders();
    response.getHeaders().forEach((key, values) -> {
      if ("X-Cache".equalsIgnoreCase(key)) {
        return;
      }
      headers.put(key, List.copyOf(values));
    });
    applyCacheHeaders(headers, route, etag, capturedAt, false);
    int status = (response.getStatusCode() != null) ? response.getStatusCode().value() : HttpStatus.OK.value();
    Instant expiresAt = capturedAt.plus(route.resolvedTtl());
    Instant staleAt = expiresAt.plus(route.resolvedStaleTtl());
    return new CachedResponse(status, headers, body, etag, capturedAt, expiresAt, staleAt);
  }

  public void applyCacheHeaders(HttpHeaders headers,
      RouteCacheProperties route,
      String etag,
      Instant cachedAt,
      boolean stale) {
    if (headers == null || route == null) {
      return;
    }
    if (StringUtils.hasText(etag)) {
      headers.setETag(etag);
    }
    Duration ttl = route.resolvedTtl();
    Duration staleTtl = route.resolvedStaleTtl();
    CacheControl control = CacheControl.maxAge(ttl)
        .staleWhileRevalidate(staleTtl)
        .cachePublic();
    headers.setCacheControl(control.getHeaderValue());
    if (cachedAt != null) {
      long ageSeconds = Math.max(0, Duration.between(cachedAt, Instant.now()).getSeconds());
      headers.set(HttpHeaders.AGE, Long.toString(ageSeconds));
    }
    if (stale) {
      headers.add("Warning", "110 - Response is stale");
    }
  }

  public String generateEtag(byte[] body) {
    if (body == null || body.length == 0) {
      return '"' + Base64.getEncoder().encodeToString(new byte[0]) + '"';
    }
    byte[] digest = DigestUtils.md5Digest(body);
    return '"' + Base64.getUrlEncoder().withoutPadding().encodeToString(digest) + '"';
  }

  public void trackRefreshStart(CacheMetadata metadata) {
    if (metadata == null) {
      return;
    }
    refreshInFlight.put(metadata.cacheKey(), Instant.now());
  }

  public void trackRefreshComplete(CacheMetadata metadata) {
    if (metadata == null) {
      return;
    }
    refreshInFlight.remove(metadata.cacheKey());
  }

  public boolean isRefreshInFlight(CacheMetadata metadata) {
    if (metadata == null) {
      return false;
    }
    return refreshInFlight.containsKey(metadata.cacheKey());
  }

  private CacheResult evaluateCachedEntry(CacheMetadata metadata,
      CachedResponse cached,
      ServerWebExchange exchange) {
    Instant now = Instant.now();
    if (cached.staleAt().isBefore(now)) {
      metrics.recordCacheMiss(metadata.route().cacheKeyPrefix(), metadata.canonicalPath());
      return CacheResult.miss(metadata);
    }
    boolean fresh = !cached.expiresAt().isBefore(now);
    CacheState state = fresh ? CacheState.FRESH : CacheState.STALE;
    metrics.recordCacheHit(metadata.route().cacheKeyPrefix(), metadata.canonicalPath(), state);
    List<String> noneMatch = exchange.getRequest().getHeaders().getIfNoneMatch();
    if (fresh && !CollectionUtils.isEmpty(noneMatch) && noneMatch.contains(cached.etag())) {
      return CacheResult.notModified(metadata, cached);
    }
    return CacheResult.hit(metadata, cached, state);
  }

  private Optional<CachedResponse> deserialize(String serialized) {
    try {
      return Optional.of(objectMapper.readValue(serialized, CachedResponse.class));
    } catch (Exception ex) {
      LOGGER.warn("Failed to deserialize cached response", ex);
      return Optional.empty();
    }
  }

  private String canonicalQuery(MultiValueMap<String, String> params) {
    if (params == null || params.isEmpty()) {
      return "-";
    }
    return params.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> {
          List<String> values = entry.getValue();
          if (values == null || values.isEmpty()) {
            return entry.getKey();
          }
          return values.stream()
              .sorted()
              .map(value -> entry.getKey() + '=' + value)
              .reduce((left, right) -> left + '&' + right)
              .orElse(entry.getKey());
        })
        .reduce((left, right) -> left + '&' + right)
        .orElse("-");
  }

  private String resolveTenant(ServerWebExchange exchange, RouteCacheProperties route) {
    if (!route.isTenantScoped()) {
      return "global";
    }
    return Optional.ofNullable(exchange.getAttribute(GatewayRequestAttributes.TENANT_ID))
        .map(Object::toString)
        .filter(StringUtils::hasText)
        .or(() -> Optional.ofNullable(ContextManager.Tenant.get()))
        .map(value -> value.toLowerCase(Locale.ROOT))
        .orElse("anonymous");
  }

  public record CacheMetadata(RouteCacheProperties route,
                              String tenantId,
                              String cacheKey,
                              String canonicalPath,
                              String canonicalQuery,
                              HttpMethod method) {
  }

  public record CacheResult(CacheMetadata metadata,
                            CachedResponse response,
                            CacheState state) {

    public static CacheResult noRoute() {
      return new CacheResult(null, null, null);
    }

    public static CacheResult miss(CacheMetadata metadata) {
      return new CacheResult(metadata, null, null);
    }

    public static CacheResult hit(CacheMetadata metadata, CachedResponse response, CacheState state) {
      return new CacheResult(metadata, response, state);
    }

    public static CacheResult notModified(CacheMetadata metadata, CachedResponse response) {
      return new CacheResult(metadata, response, CacheState.NOT_MODIFIED);
    }

    public boolean hasRoute() {
      return metadata != null && metadata.route() != null;
    }

    public boolean isMiss() {
      return hasRoute() && response == null;
    }
  }

  public record CachedResponse(@JsonProperty("status") int status,
                               @JsonProperty("headers") HttpHeaders headers,
                               @JsonProperty("body") byte[] body,
                               @JsonProperty("etag") String etag,
                               @JsonProperty("cachedAt") Instant cachedAt,
                               @JsonProperty("expiresAt") Instant expiresAt,
                               @JsonProperty("staleAt") Instant staleAt) {
    @JsonCreator
    public CachedResponse {
      Objects.requireNonNull(headers, "headers");
      Objects.requireNonNull(body, "body");
      Objects.requireNonNull(etag, "etag");
      Objects.requireNonNull(cachedAt, "cachedAt");
      Objects.requireNonNull(expiresAt, "expiresAt");
      Objects.requireNonNull(staleAt, "staleAt");
    }
  }
}
