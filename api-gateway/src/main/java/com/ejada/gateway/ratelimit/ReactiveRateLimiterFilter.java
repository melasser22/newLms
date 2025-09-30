package com.ejada.gateway.ratelimit;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.common.dto.BaseResponse;
import com.ejada.shared_starter_ratelimit.RateLimitProps;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
  private final ObjectMapper objectMapper;

  @Autowired
  public ReactiveRateLimiterFilter(ReactiveStringRedisTemplate redisTemplate,
      RateLimitProps props,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> jacksonObjectMapper,
      ObjectProvider<ObjectMapper> objectMapperProvider) {
    this(redisTemplate, props, resolveObjectMapper(jacksonObjectMapper, objectMapperProvider));
  }

  public ReactiveRateLimiterFilter(ReactiveStringRedisTemplate redisTemplate,
      RateLimitProps props,
      @Nullable ObjectMapper objectMapper) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
    this.props = Objects.requireNonNull(props, "props");
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String key = keyFor(exchange);
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
            })))
        .onErrorResume(ex -> {
          LOGGER.warn("Rate limiter failed, allowing request", ex);
          return chain.filter(exchange);
        });
  }

  private Mono<Boolean> setExpiry(String bucket, Long count, Duration window) {
    if (count != null && count == 1L) {
      return redisTemplate.expire(bucket, window);
    }
    return Mono.just(Boolean.TRUE);
  }

  private Duration resolveWindow() {
    int capacity = Math.max(1, props.getCapacity());
    int refillPerMinute = Math.max(1, props.getRefillPerMinute());
    long seconds = (long) Math.ceil(capacity * 60.0 / refillPerMinute);
    if (seconds <= 0L) {
      seconds = 1L;
    }
    return Duration.ofSeconds(seconds);
  }

  private String keyFor(ServerWebExchange exchange) {
    return switch (props.getKeyStrategy()) {
      case "ip" -> {
        String forwarded = exchange.getRequest().getHeaders().getFirst(HeaderNames.CLIENT_IP);
        yield StringUtils.hasText(forwarded)
            ? forwarded
            : Objects.toString(exchange.getRequest().getRemoteAddress(), "anonymous");
      }
      case "user" -> {
        String userId = ContextManager.getUserId();
        yield StringUtils.hasText(userId) ? userId : "anonymous";
      }
      default -> {
        String tenant = ContextManager.Tenant.get();
        yield StringUtils.hasText(tenant) ? tenant : "public";
      }
    };
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
}
