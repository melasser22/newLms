package com.ejada.gateway.security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.GatewaySecurityProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Validates X-API-Key headers against Redis backed records and populates tenant context.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiKeyAuthenticationFilter implements WebFilter, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

  private final ReactiveStringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final GatewaySecurityProperties properties;
  private final GatewaySecurityMetrics metrics;

  public ApiKeyAuthenticationFilter(
      ReactiveStringRedisTemplate redisTemplate,
      GatewaySecurityMetrics metrics,
      GatewaySecurityProperties properties,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
    this.properties = Objects.requireNonNull(properties, "properties");
    this.metrics = Objects.requireNonNull(metrics, "metrics");
    ObjectMapper mapper = (primaryObjectMapper != null) ? primaryObjectMapper.getIfAvailable() : null;
    if (mapper == null) {
      mapper = (fallbackObjectMapper != null) ? fallbackObjectMapper.getIfAvailable() : null;
    }
    this.objectMapper = Objects.requireNonNull(mapper, "objectMapper");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (!properties.getApiKey().isEnabled()) {
      return chain.filter(exchange);
    }

    String apiKey = trimToNull(exchange.getRequest().getHeaders().getFirst(HeaderNames.API_KEY));
    if (!StringUtils.hasText(apiKey)) {
      return chain.filter(exchange);
    }

    String redisKey = properties.getApiKey().redisKey(apiKey);
    return redisTemplate.opsForValue().get(redisKey)
        .flatMap(json -> Mono.fromCallable(() -> objectMapper.readValue(json, ApiKeyRecord.class))
            .onErrorResume(ex -> {
              LOGGER.warn("Failed to decode API key payload", ex);
              return Mono.empty();
            }))
        .switchIfEmpty(Mono.defer(() -> {
          LOGGER.debug("API key not found for redis key {}", redisKey);
          metrics.incrementBlocked("api_key", null);
          return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_API_KEY_INVALID", "Invalid API key")
              .then(Mono.empty());
        }))
        .flatMap(record -> validateAndAuthenticate(record, apiKey, exchange, chain));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }

  private Mono<Void> validateAndAuthenticate(ApiKeyRecord record, String apiKey,
      ServerWebExchange exchange, WebFilterChain chain) {
    if (!StringUtils.hasText(record.tenantId())) {
      metrics.incrementBlocked("api_key", null);
      return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_API_KEY_INVALID", "API key is not bound to a tenant");
    }
    if (record.expiresAt() != null && record.expiresAt().isBefore(Instant.now())) {
      metrics.incrementBlocked("api_key_expired", record.tenantId());
      return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_API_KEY_EXPIRED", "API key has expired");
    }

    Collection<GrantedAuthority> authorities = authorities(record.scopes());
    ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(apiKey, record.tenantId(), authorities);

    metrics.incrementApiKeyValidated(record.tenantId());

    ServerHttpRequest mutatedRequest = mutateRequest(exchange.getRequest(), record.tenantId());
    ServerWebExchange mutatedExchange = exchange.mutate()
        .request(mutatedRequest)
        .build();
    mutatedExchange.getAttributes().put(GatewayRequestAttributes.TENANT_ID, record.tenantId());
    mutatedExchange.getAttributes().put(HeaderNames.X_TENANT_ID, record.tenantId());

    return chain.filter(mutatedExchange)
        .contextWrite(ctx -> ctx
            .put(GatewayRequestAttributes.TENANT_ID, record.tenantId())
            .put(HeaderNames.X_TENANT_ID, record.tenantId())
            .put(SecurityContext.class, new SecurityContextImpl(authentication)))
        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
  }

  private ServerHttpRequest mutateRequest(ServerHttpRequest request, String tenantId) {
    if (StringUtils.hasText(request.getHeaders().getFirst(HeaderNames.X_TENANT_ID))) {
      return request;
    }
    return request.mutate()
        .header(HeaderNames.X_TENANT_ID, tenantId)
        .build();
  }

  private Collection<GrantedAuthority> authorities(Set<String> scopes) {
    if (scopes == null || scopes.isEmpty()) {
      return List.of();
    }
    return scopes.stream()
        .filter(StringUtils::hasText)
        .map(scope -> scope.startsWith("SCOPE_") ? scope : "SCOPE_" + scope)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toUnmodifiableList());
  }

  private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String code, String message) {
    var response = exchange.getResponse();
    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    BaseResponse<Void> body = BaseResponse.error(code, message);
    byte[] payload;
    try {
      payload = objectMapper.writeValueAsBytes(body);
    } catch (Exception ex) {
      payload = body.toString().getBytes(StandardCharsets.UTF_8);
    }
    return response.writeWith(Mono.just(response.bufferFactory().wrap(payload)));
  }

  private static String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private record ApiKeyRecord(String tenantId, Set<String> scopes, Instant expiresAt) {
  }
}
