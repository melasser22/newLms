package com.ejada.gateway.security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.config.GatewayRoutesProperties.ServiceRoute;
import com.ejada.gateway.config.GatewaySecurityProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.security.GatewaySecurityMetrics;
import com.ejada.gateway.security.apikey.ApiKeyCodec;
import com.ejada.gateway.security.apikey.ApiKeyCodec.DecodedApiKeyRecord;
import com.ejada.gateway.security.apikey.ApiKeyRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
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
  private final GatewayRoutesProperties routesProperties;
  private final PathMatcher pathMatcher = new AntPathMatcher();
  private final ApiKeyCodec apiKeyCodec;

  public ApiKeyAuthenticationFilter(
      ReactiveStringRedisTemplate redisTemplate,
      GatewaySecurityMetrics metrics,
      GatewaySecurityProperties properties,
      GatewayRoutesProperties routesProperties,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper,
      ApiKeyCodec apiKeyCodec) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
    this.properties = Objects.requireNonNull(properties, "properties");
    this.metrics = Objects.requireNonNull(metrics, "metrics");
    this.routesProperties = Objects.requireNonNull(routesProperties, "routesProperties");
    ObjectMapper mapper = (primaryObjectMapper != null) ? primaryObjectMapper.getIfAvailable() : null;
    if (mapper == null) {
      mapper = (fallbackObjectMapper != null) ? fallbackObjectMapper.getIfAvailable() : null;
    }
    this.objectMapper = Objects.requireNonNull(mapper, "objectMapper");
    this.apiKeyCodec = Objects.requireNonNull(apiKeyCodec, "apiKeyCodec");
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
        .flatMap(payload -> decodeRecord(payload)
            .onErrorResume(ex -> {
              LOGGER.warn("Failed to decode API key payload for key {}", redisKey, ex);
              metrics.incrementBlocked("api_key_decode", null);
              return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_API_KEY_INVALID", "Invalid API key")
                  .then(Mono.<DecodedApiKeyRecord>empty());
            }))
        .switchIfEmpty(Mono.defer(() -> {
          LOGGER.debug("API key not found for redis key {}", redisKey);
          metrics.incrementBlocked("api_key", null);
          return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_API_KEY_INVALID", "Invalid API key")
              .then(Mono.<DecodedApiKeyRecord>empty());
        }))
        .flatMap(decoded -> validateAndAuthenticate(decoded, apiKey, redisKey, exchange, chain))
        .onErrorResume(ex -> handleRedisFailure(exchange, chain, redisKey, ex));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }

  private Mono<DecodedApiKeyRecord> decodeRecord(String raw) {
    return apiKeyCodec.decode(raw)
        .onErrorResume(ex -> Mono.error(new IllegalStateException("Failed to decode API key", ex)));
  }

  private Mono<Void> validateAndAuthenticate(DecodedApiKeyRecord decoded, String apiKey, String redisKey,
      ServerWebExchange exchange, WebFilterChain chain) {
    ApiKeyRecord record = decoded.record();
    Instant now = Instant.now();
    if (!StringUtils.hasText(record.getTenantId())) {
      metrics.incrementBlocked("api_key", null);
      return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_API_KEY_INVALID", "API key is not bound to a tenant");
    }
    if (record.getExpiresAt() != null && record.getExpiresAt().isBefore(now)) {
      metrics.incrementBlocked("api_key_expired", record.getTenantId());
      return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_API_KEY_EXPIRED", "API key has expired");
    }

    GatewaySecurityProperties.ApiKey.Rotation rotation = properties.getApiKey().getRotation();
    if (rotation.isEnabled()) {
      Instant rotatedAt = record.getRotatedAt();
      if (rotatedAt == null) {
        metrics.incrementBlocked("api_key_rotation_missing", record.getTenantId());
        return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_API_KEY_ROTATION_REQUIRED", "API key rotation metadata missing");
      }
      Duration maxAge = Duration.ofDays(Math.max(1, rotation.getMaxAgeDays()));
      if (rotatedAt.plus(maxAge).isBefore(now)) {
        metrics.incrementBlocked("api_key_rotation_expired", record.getTenantId());
        return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_API_KEY_ROTATED", "API key rotation window exceeded");
      }
    }

    ScopeRequirements requirements = resolveScopeRequirements(exchange);
    if (!requirements.scopes().isEmpty()) {
      Set<String> granted = normaliseScopes(record.getScopes());
      Set<String> required = normaliseScopes(requirements.scopes());
      boolean requireExact = properties.getApiKey().getScopeEnforcement().isRequireExactMatch();
      boolean authorised = requireExact ? granted.equals(required) : granted.containsAll(required);
      if (!authorised) {
        metrics.incrementBlocked("api_key_scope", record.getTenantId());
        String message = requireExact
            ? "API key scopes do not exactly match route requirements"
            : "API key missing required scopes";
        return reject(exchange, HttpStatus.FORBIDDEN, "ERR_API_KEY_SCOPE", message);
      }
    }

    Collection<GrantedAuthority> authorities = authorities(record.getScopes());
    ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(apiKey, record.getTenantId(), authorities);

    return enforceRateLimit(apiKey, record, exchange)
        .flatMap(allowed -> {
          if (!allowed) {
            return Mono.empty();
          }

          metrics.incrementApiKeyValidated(record.getTenantId());

          ServerHttpRequest mutatedRequest = mutateRequest(exchange.getRequest(), record.getTenantId());
          ServerWebExchange mutatedExchange = exchange.mutate()
              .request(mutatedRequest)
              .build();
          exchange.getAttributes().put(GatewayRequestAttributes.TENANT_ID, record.getTenantId());
          exchange.getAttributes().put(HeaderNames.X_TENANT_ID, record.getTenantId());
          exchange.getAttributes().put(GatewayRequestAttributes.MUTATED_REQUEST, mutatedRequest);
          mutatedExchange.getAttributes().put(GatewayRequestAttributes.TENANT_ID, record.getTenantId());
          mutatedExchange.getAttributes().put(HeaderNames.X_TENANT_ID, record.getTenantId());
          mutatedExchange.getAttributes().put(GatewayRequestAttributes.MUTATED_REQUEST, mutatedRequest);
          if (!StringUtils.hasText(exchange.getRequest().getHeaders().getFirst(HeaderNames.X_TENANT_ID))) {
            try {
              exchange.getRequest().getHeaders().set(HeaderNames.X_TENANT_ID, record.getTenantId());
            } catch (UnsupportedOperationException ignored) {
              // Some request implementations expose read-only headers; downstream exchange already mutated.
            }
          }

          Mono<Void> audit = auditUsage(decoded, redisKey, record, exchange)
              .onErrorResume(ex -> {
                LOGGER.warn("Failed to audit API key usage for tenant {}", record.getTenantId(), ex);
                return Mono.empty();
              });

          return audit.then(chain.filter(mutatedExchange)
              .contextWrite(ctx -> ctx
                  .put(GatewayRequestAttributes.TENANT_ID, record.getTenantId())
                  .put(HeaderNames.X_TENANT_ID, record.getTenantId())
                  .put(SecurityContext.class, new SecurityContextImpl(authentication)))
              .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)));
        });
  }

  private Mono<Void> auditUsage(DecodedApiKeyRecord decoded, String redisKey, ApiKeyRecord record,
      ServerWebExchange exchange) {
    GatewaySecurityProperties.ApiKey.Audit audit = properties.getApiKey().getAudit();
    if (!audit.isLogUsage() && !audit.isTrackLastUsed()) {
      return Mono.empty();
    }
    Instant now = Instant.now();
    ScopeRequirements requirements = resolveScopeRequirements(exchange);
    if (audit.isLogUsage()) {
      LOGGER.info("API key usage: tenant={}, path={}, routes={}, keyId={}",
          record.getTenantId(), exchange.getRequest().getPath(),
          String.join(",", requirements.routeIds()), decoded.keyId().orElse("n/a"));
    }
    if (!audit.isTrackLastUsed()) {
      return Mono.empty();
    }
    Instant previous = record.getLastUsedAt();
    if (previous != null && !previous.isBefore(now.minusSeconds(1))) {
      return Mono.empty();
    }
    record.setLastUsedAt(now);
    return Mono.fromCallable(() -> apiKeyCodec.encode(record, decoded.encrypted()))
        .flatMap(serialized -> {
          if (!StringUtils.hasText(serialized)) {
            return Mono.<Void>empty();
          }
          return redisTemplate.opsForValue().set(redisKey, serialized)
              .onErrorResume(ex -> handleRedisFailure(ex, "set", redisKey))
              .then();
        })
        .onErrorResume(ex -> {
          if (isRedisConnectivityIssue(ex)) {
            LOGGER.warn("Failed to update API key usage metadata for tenant {} due to Redis outage", record.getTenantId(), ex);
            return Mono.empty();
          }
          return Mono.error(ex);
        });
  }

  private Mono<Boolean> enforceRateLimit(String apiKey, ApiKeyRecord record, ServerWebExchange exchange) {
    GatewaySecurityProperties.ApiKey.RateLimit cfg = properties.getApiKey().getRateLimit();
    if (!cfg.isEnabled()) {
      return Mono.just(true);
    }
    long resolvedAllowed = 0L;
    if (record.getRateLimitPerMinute() != null) {
      resolvedAllowed = Math.max(0, record.getRateLimitPerMinute());
    }
    if (resolvedAllowed <= 0) {
      resolvedAllowed = Math.max(0, cfg.getDefaultPerMinute());
    }
    final long allowed = resolvedAllowed;
    if (allowed <= 0) {
      return Mono.just(true);
    }
    String rateKey = cfg.redisKey(apiKey);
    return redisTemplate.opsForValue().increment(rateKey)
        .onErrorResume(ex -> handleRedisFailure(ex, "increment", rateKey))
        .flatMap(count -> {
          if (count != null && count == 1L) {
            return redisTemplate.expire(rateKey, Duration.ofMinutes(1))
                .onErrorResume(expireEx -> handleRedisFailure(expireEx, "expire", rateKey))
                .thenReturn(count);
          }
          return Mono.just(count);
        })
        .defaultIfEmpty(1L)
        .flatMap(count -> {
          if (count > allowed) {
            metrics.incrementBlocked("api_key_rate_limit", record.getTenantId());
            return reject(exchange, HttpStatus.TOO_MANY_REQUESTS, "ERR_API_KEY_RATE_LIMIT", "API key rate limit exceeded")
                .thenReturn(false);
          }
          return Mono.just(true);
        });
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
    if (response.isCommitted()) {
      LOGGER.debug("Response already committed, skipping API key rejection for {}", exchange.getRequest().getPath());
      return Mono.empty();
    }
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

  private ScopeRequirements resolveScopeRequirements(ServerWebExchange exchange) {
    GatewaySecurityProperties.ApiKey.ScopeEnforcement enforcement = properties.getApiKey().getScopeEnforcement();
    if (!enforcement.isEnabled()) {
      return ScopeRequirements.empty();
    }
    String path = exchange.getRequest().getURI().getPath();
    HttpMethod method = exchange.getRequest().getMethod();
    Set<String> scopes = new LinkedHashSet<>();
    Set<String> routeIds = new LinkedHashSet<>();
    for (ServiceRoute route : routesProperties.getRoutes().values()) {
      if (route == null || route.getRequiredScopes().isEmpty()) {
        continue;
      }
      if (!methodMatches(route, method)) {
        continue;
      }
      if (!pathMatches(route, path)) {
        continue;
      }
      scopes.addAll(route.getRequiredScopes());
      if (StringUtils.hasText(route.getId())) {
        routeIds.add(route.getId());
      }
    }
    return scopes.isEmpty() ? ScopeRequirements.empty() : new ScopeRequirements(scopes, routeIds);
  }

  private boolean methodMatches(ServiceRoute route, HttpMethod method) {
    if (route.getMethods() == null || route.getMethods().isEmpty()) {
      return true;
    }
    if (method == null) {
      return false;
    }
    String current = method.name().toUpperCase(Locale.ROOT);
    return route.getMethods().stream()
        .filter(StringUtils::hasText)
        .map(value -> value.trim().toUpperCase(Locale.ROOT))
        .anyMatch(current::equals);
  }

  private boolean pathMatches(ServiceRoute route, String path) {
    if (route.getPaths() == null || route.getPaths().isEmpty()) {
      return false;
    }
    for (String pattern : route.getPaths()) {
      if (StringUtils.hasText(pattern) && pathMatcher.match(pattern, path)) {
        return true;
      }
    }
    return false;
  }

  private Set<String> normaliseScopes(Collection<String> scopes) {
    if (scopes == null || scopes.isEmpty()) {
      return Set.of();
    }
    return scopes.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .map(scope -> scope.startsWith("SCOPE_") ? scope.substring("SCOPE_".length()) : scope)
        .map(value -> value.toLowerCase(Locale.ROOT))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Mono<Void> handleRedisFailure(ServerWebExchange exchange, WebFilterChain chain, String redisKey, Throwable ex) {
    if (!isRedisConnectivityIssue(ex)) {
      return Mono.error(ex);
    }
    LOGGER.warn("Redis unavailable during API key lookup for key {}, allowing request", redisKey, ex);
    return chain.filter(exchange);
  }

  private <T> Mono<T> handleRedisFailure(Throwable ex, String operation, String key) {
    if (!isRedisConnectivityIssue(ex)) {
      return Mono.error(ex);
    }
    LOGGER.warn("Redis unavailable during API key {} for key {}, continuing without enforcement", operation, key, ex);
    return Mono.empty();
  }

  private boolean isRedisConnectivityIssue(Throwable ex) {
    Throwable current = ex;
    while (current != null) {
      if (current instanceof RedisConnectionFailureException
          || current instanceof DataAccessResourceFailureException
          || current instanceof ConnectException
          || current instanceof UnknownHostException
          || current instanceof SocketTimeoutException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private static String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private record ScopeRequirements(Set<String> scopes, Set<String> routeIds) {
    static ScopeRequirements empty() {
      return new ScopeRequirements(Set.of(), Set.of());
    }
  }
}
