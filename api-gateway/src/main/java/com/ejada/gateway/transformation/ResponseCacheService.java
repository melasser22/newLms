package com.ejada.gateway.transformation;

import com.ejada.common.context.ContextManager;
import com.ejada.gateway.config.GatewayCacheProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.metrics.GatewayMetrics;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
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

  public Mono<Optional<CachedResponse>> find(String routeId, ServerWebExchange exchange) {
    if (!isCacheEnabled()) {
      return Mono.just(Optional.empty());
    }
    Duration ttl = properties.resolveTtl(routeId);
    if (ttl == null || ttl.isZero() || ttl.isNegative()) {
      return Mono.just(Optional.empty());
    }
    String key = buildKey(routeId, exchange);
    return redisTemplate.opsForValue().get(key)
        .flatMap(serialized -> deserialize(serialized)
            .map(value -> {
              metrics.recordCacheHit();
              return Mono.just(Optional.of(value));
            })
            .orElseGet(() -> {
              metrics.recordCacheMiss();
              return Mono.just(Optional.empty());
            }))
        .switchIfEmpty(Mono.defer(() -> {
          metrics.recordCacheMiss();
          return Mono.empty();
        }))
        .switchIfEmpty(Mono.just(Optional.empty()));
  }

  public Mono<Void> store(String routeId, ServerWebExchange exchange, CachedResponse response) {
    if (!isCacheEnabled()) {
      return Mono.empty();
    }
    Duration ttl = properties.resolveTtl(routeId);
    if (ttl == null || ttl.isZero() || ttl.isNegative()) {
      return Mono.empty();
    }
    String key = buildKey(routeId, exchange);
    try {
      String serialized = objectMapper.writeValueAsString(response);
      return redisTemplate.opsForValue()
          .set(key, serialized, ttl)
          .doOnError(ex -> LOGGER.warn("Failed to store cached response for {}", key, ex))
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
    String key = buildKey(routeId, exchange);
    return redisTemplate.opsForValue()
        .delete(key)
        .then();
  }

  public CachedResponse snapshotResponse(ServerHttpResponse response, byte[] body) {
    HttpHeaders headers = new HttpHeaders();
    response.getHeaders().forEach((key, values) -> {
      if ("X-Cache".equalsIgnoreCase(key)) {
        return;
      }
      headers.put(key, List.copyOf(values));
    });
    int status = (response.getStatusCode() != null) ? response.getStatusCode().value() : 200;
    return new CachedResponse(status, headers, body);
  }

  private String buildKey(String routeId, ServerWebExchange exchange) {
    StringBuilder builder = new StringBuilder(CACHE_PREFIX).append(routeId).append(':');
    builder.append(exchange.getRequest().getPath().pathWithinApplication().value());
    builder.append(':').append(canonicalQuery(exchange.getRequest().getQueryParams()));
    String tenantId = Optional.ofNullable(exchange.getAttribute(GatewayRequestAttributes.TENANT_ID))
        .map(Object::toString)
        .filter(value -> !value.isBlank())
        .orElseGet(() -> Optional.ofNullable(ContextManager.Tenant.get()).orElse("anonymous"));
    builder.append(':').append(tenantId);
    return builder.toString();
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

  private Optional<CachedResponse> deserialize(String serialized) {
    try {
      return Optional.of(objectMapper.readValue(serialized, CachedResponse.class));
    } catch (Exception ex) {
      LOGGER.warn("Failed to deserialize cached response", ex);
      return Optional.empty();
    }
  }

  public record CachedResponse(@JsonProperty("status") int status,
                               @JsonProperty("headers") HttpHeaders headers,
                               @JsonProperty("body") byte[] body) {
    @JsonCreator
    public CachedResponse {
      Objects.requireNonNull(headers, "headers");
      Objects.requireNonNull(body, "body");
    }
  }
}

