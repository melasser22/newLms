package com.ejada.gateway.security;

import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.GatewaySecurityProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Performs token introspection with Redis caching for JWT revocation checks.
 */
public class GatewayTokenIntrospectionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(GatewayTokenIntrospectionService.class);
  private static final ParameterizedTypeReference<BaseResponse<TokenIntrospectionPayload>> RESPONSE_TYPE =
      new ParameterizedTypeReference<>() { };

  private final GatewaySecurityProperties properties;
  private final ReactiveStringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final WebClient webClient;
  private final GatewaySecurityMetrics metrics;

  public GatewayTokenIntrospectionService(
      GatewaySecurityProperties properties,
      ReactiveStringRedisTemplate redisTemplate,
      GatewaySecurityMetrics metrics,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper,
      WebClient.Builder webClientBuilder) {
    this.properties = Objects.requireNonNull(properties, "properties");
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
    this.metrics = Objects.requireNonNull(metrics, "metrics");
    ObjectMapper mapper = (primaryObjectMapper != null) ? primaryObjectMapper.getIfAvailable() : null;
    if (mapper == null) {
      mapper = (fallbackObjectMapper != null) ? fallbackObjectMapper.getIfAvailable() : null;
    }
    this.objectMapper = Objects.requireNonNull(mapper, "objectMapper");
    this.webClient = webClientBuilder.clone().build();
  }

  public Mono<Void> verify(String tokenValue, Jwt jwt) {
    if (!properties.getTokenCache().isEnabled()) {
      return Mono.empty();
    }
    String jti = resolveJti(jwt);
    if (!StringUtils.hasText(jti)) {
      return Mono.empty();
    }
    String cacheKey = properties.getTokenCache().redisKey(jti);
    return redisTemplate.opsForValue().get(cacheKey)
        .flatMap(json -> decodeStatus(json).switchIfEmpty(Mono.defer(() -> fetchAndCache(jti, cacheKey, jwt))))
        .switchIfEmpty(Mono.defer(() -> fetchAndCache(jti, cacheKey, jwt)))
        .flatMap(status -> evaluateStatus(status, jwt, jti));
  }

  private Mono<TokenStatus> fetchAndCache(String jti, String cacheKey, Jwt jwt) {
    return fetchStatus(jti)
        .flatMap(status -> cacheStatus(cacheKey, status, jwt).onErrorResume(ex -> Mono.empty()).thenReturn(status))
        .onErrorResume(ex -> {
          LOGGER.warn("Failed to introspect token {}", jti, ex);
          return Mono.just(TokenStatus.active(null, null));
        });
  }

  private Mono<TokenStatus> decodeStatus(String json) {
    return Mono.fromCallable(() -> objectMapper.readValue(json, TokenStatus.class))
        .onErrorResume(ex -> {
          LOGGER.warn("Failed to decode cached token status", ex);
          return Mono.empty();
        });
  }

  private Mono<Void> cacheStatus(String cacheKey, TokenStatus status, Jwt jwt) {
    Duration ttl = resolveTtl(status, jwt);
    if (ttl == null || ttl.isZero() || ttl.isNegative()) {
      return Mono.empty();
    }
    try {
      String payload = objectMapper.writeValueAsString(status);
      return redisTemplate.opsForValue().set(cacheKey, payload, ttl).then();
    } catch (JsonProcessingException e) {
      LOGGER.debug("Failed to serialize token status for cache", e);
      return Mono.empty();
    }
  }

  private Duration resolveTtl(TokenStatus status, Jwt jwt) {
    Instant now = Instant.now();
    Instant candidate = status.expiresAt();
    if (candidate == null) {
      candidate = jwt.getExpiresAt();
    }
    Duration ttl = (candidate != null) ? Duration.between(now, candidate) : null;
    Duration configured = properties.getTokenCache().getTtl();
    if (configured != null && !configured.isZero() && !configured.isNegative()) {
      if (ttl == null || ttl.compareTo(configured) > 0) {
        ttl = configured;
      }
    }
    return ttl;
  }

  private Mono<TokenStatus> fetchStatus(String jti) {
    return webClient.get()
        .uri(properties.getTokenCache().getRevocationUri(), jti)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(RESPONSE_TYPE)
        .map(this::mapPayload)
        .switchIfEmpty(Mono.just(TokenStatus.active(null, null)));
  }

  private TokenStatus mapPayload(BaseResponse<TokenIntrospectionPayload> response) {
    Object rawPayload = response != null ? response.getData() : null;
    Boolean active = null;
    Boolean revoked = null;
    Instant expiresAt = null;
    String tenantId = null;
    if (rawPayload instanceof TokenIntrospectionPayload typed) {
      active = typed.active;
      revoked = typed.revoked;
      expiresAt = typed.expiresAt;
      tenantId = typed.tenantId;
    } else if (rawPayload instanceof Map<?, ?> map) {
      active = booleanValue(map.get("active"));
      revoked = booleanValue(map.get("revoked"));
      expiresAt = instantValue(map.get("expiresAt"));
      tenantId = stringValue(map.get("tenantId"));
    } else if (rawPayload != null) {
      try {
        TokenIntrospectionPayload converted = objectMapper.convertValue(rawPayload, TokenIntrospectionPayload.class);
        active = converted.active;
        revoked = converted.revoked;
        expiresAt = converted.expiresAt;
        tenantId = converted.tenantId;
      } catch (IllegalArgumentException ex) {
        LOGGER.warn("Failed to convert introspection payload", ex);
      }
    }
    boolean resolvedActive = active != null ? active : !Boolean.TRUE.equals(revoked);
    if (Boolean.TRUE.equals(revoked)) {
      resolvedActive = false;
    }
    return new TokenStatus(resolvedActive, trimToNull(tenantId), expiresAt);
  }

  private Boolean booleanValue(Object value) {
    if (value instanceof Boolean bool) {
      return bool;
    }
    if (value instanceof String text) {
      if (StringUtils.hasText(text)) {
        return Boolean.parseBoolean(text.trim());
      }
    }
    return null;
  }

  private Instant instantValue(Object value) {
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof String text && StringUtils.hasText(text)) {
      try {
        return Instant.parse(text.trim());
      } catch (Exception ignored) {
        return null;
      }
    }
    return null;
  }

  private String stringValue(Object value) {
    if (value == null) {
      return null;
    }
    return value.toString();
  }

  private Mono<Void> evaluateStatus(TokenStatus status, Jwt jwt, String jti) {
    if (!status.active()) {
      metrics.incrementBlocked("token", status.tenantId());
      return Mono.error(new JwtException("Token " + jti + " is revoked"));
    }
    return Mono.empty();
  }

  private String resolveJti(Jwt jwt) {
    String jti = trimToNull(jwt.getId());
    if (!StringUtils.hasText(jti)) {
      Object claim = jwt.getClaims().get("jti");
      if (claim != null) {
        jti = trimToNull(claim.toString());
      }
    }
    return jti;
  }

  private static String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private record TokenStatus(boolean active, String tenantId, Instant expiresAt) {
    static TokenStatus active(Instant expiresAt, String tenantId) {
      return new TokenStatus(true, tenantId, expiresAt);
    }
  }

  private static class TokenIntrospectionPayload {
    private Boolean active;
    private Boolean revoked;
    private Instant expiresAt;
    private String tenantId;
  }
}
