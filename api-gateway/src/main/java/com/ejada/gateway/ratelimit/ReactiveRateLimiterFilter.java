package com.ejada.gateway.ratelimit;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.common.dto.BaseResponse;
import com.ejada.shared_starter_ratelimit.RateLimitProps;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive adaptation of the servlet {@code RateLimitFilter}. It uses Redis
 * atomic increments to enforce a simple fixed window rate limit per strategy.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ReactiveRateLimiterFilter implements WebFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveRateLimiterFilter.class);

  private final ReactiveStringRedisTemplate redisTemplate;
  private final RateLimitProps props;
  private final KeyResolver keyResolver;
  private final ObjectMapper objectMapper;

  @Autowired
  public ReactiveRateLimiterFilter(ReactiveStringRedisTemplate redisTemplate,
      RateLimitProps props,
      KeyResolver keyResolver,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> jacksonObjectMapper,
      ObjectProvider<ObjectMapper> objectMapperProvider) {
    this(redisTemplate, props, keyResolver, resolveObjectMapper(jacksonObjectMapper, objectMapperProvider));
  }

  public ReactiveRateLimiterFilter(ReactiveStringRedisTemplate redisTemplate,
      RateLimitProps props,
      KeyResolver keyResolver,
      @Nullable ObjectMapper objectMapper) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
    this.props = Objects.requireNonNull(props, "props");
    this.keyResolver = Objects.requireNonNull(keyResolver, "keyResolver");
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    return resolveKey(exchange)
        .flatMap(key -> applyRateLimit(exchange, chain, key))
        .onErrorResume(ex -> {
          LOGGER.warn("Rate limiter failed, allowing request", ex);
          return chain.filter(exchange);
        });
  }

  private Mono<Void> applyRateLimit(ServerWebExchange exchange, WebFilterChain chain, String key) {
    String bucket = "rl:" + key;
    Duration window = resolveWindow();
    return redisTemplate.opsForValue().increment(bucket)
        .flatMap(count -> setExpiry(bucket, count, window)
            .then(Mono.defer(() -> {
              int capacity = Math.max(1, props.getCapacity());
              exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(capacity));
              exchange.getResponse().getHeaders().set("X-RateLimit-Remaining",
                  String.valueOf(Math.max(0, capacity - count.intValue())));
              if (count > capacity) {
                return reject(exchange);
              }
              return chain.filter(exchange);
            })));
  }

  private Mono<Boolean> setExpiry(String bucket, Long count, Duration window) {
    if (count != null && count == 1L) {
      return redisTemplate.expire(bucket, window);
    }
    return Mono.just(Boolean.TRUE);
  }

  private Duration resolveWindow() {
    Duration configured = props.getWindow();
    if (configured == null || configured.isZero() || configured.isNegative()) {
      return Duration.ofMinutes(1);
    }
    return configured;
  }

  private Mono<String> resolveKey(ServerWebExchange exchange) {
    return keyResolver.resolve(exchange)
        .flatMap(value -> Mono.justOrEmpty(trimToNull(value)))
        .map(this::normalizeKey)
        .switchIfEmpty(Mono.fromCallable(() -> normalizeKey(resolveFallbackKey(exchange))))
        .onErrorResume(ex -> {
          LOGGER.warn("Key resolver {} failed, falling back to legacy resolution", keyResolver.getClass().getSimpleName(), ex);
          return Mono.fromCallable(() -> normalizeKey(resolveFallbackKey(exchange)));
        });
  }

  private String resolveFallbackKey(ServerWebExchange exchange) {
    return switch (props.getKeyStrategy()) {
      case "ip" -> {
        String forwarded = exchange.getRequest().getHeaders().getFirst(HeaderNames.CLIENT_IP);
        yield StringUtils.hasText(forwarded)
            ? forwarded
            : Objects.toString(exchange.getRequest().getRemoteAddress(), DEFAULT_FALLBACK_KEY);
      }
      case "user" -> {
        String userId = ContextManager.getUserId();
        yield StringUtils.hasText(userId) ? userId : DEFAULT_FALLBACK_KEY;
      }
      default -> {
        String tenant = ContextManager.Tenant.get();
        yield StringUtils.hasText(tenant) ? tenant : "public";
      }
    };
  }

  private String normalizeKey(String key) {
    if (!StringUtils.hasText(key)) {
      return DEFAULT_FALLBACK_KEY;
    }
    return key.trim().toLowerCase(Locale.ROOT);
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private Mono<Void> reject(ServerWebExchange exchange) {
    LOGGER.debug("Rate limit exceeded for request to {}", exchange.getRequest().getPath());
    var response = exchange.getResponse();
    response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    BaseResponse<Void> body = BaseResponse.error("ERR_RATE_LIMIT", "Rate limit exceeded");
    byte[] payload;
    try {
      payload = (objectMapper != null)
          ? objectMapper.writeValueAsBytes(body)
          : body.toString().getBytes(StandardCharsets.UTF_8);
    } catch (JsonProcessingException e) {
      payload = body.toString().getBytes(StandardCharsets.UTF_8);
    }
    return response.writeWith(Mono.just(response.bufferFactory().wrap(payload)));
  }

  private static ObjectMapper resolveObjectMapper(ObjectProvider<ObjectMapper> primary,
      ObjectProvider<ObjectMapper> fallback) {
    ObjectMapper mapper = (primary != null) ? primary.getIfAvailable() : null;
    if (mapper != null) {
      return mapper;
    }
    return (fallback != null) ? fallback.getIfAvailable() : null;
  }

  private static final String DEFAULT_FALLBACK_KEY = "anonymous";
}
