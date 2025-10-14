package com.ejada.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Provides idempotency guarantees by caching responses for duplicate requests
 * that supply an {@code X-Idempotency-Key} header.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class IdempotencyDeduplicationFilter implements GlobalFilter, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdempotencyDeduplicationFilter.class);

  private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";

  private static final String REDIS_PREFIX = "gateway:idempotency:";

  private static final Duration CACHE_TTL = Duration.ofHours(24);

  private final ReactiveStringRedisTemplate redisTemplate;

  private final ObjectMapper objectMapper;

  public IdempotencyDeduplicationFilter(ReactiveStringRedisTemplate redisTemplate,
      ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (redisTemplate == null) {
      return chain.filter(exchange);
    }
    String rawKey = exchange.getRequest().getHeaders().getFirst(IDEMPOTENCY_HEADER);
    if (!StringUtils.hasText(rawKey)) {
      return chain.filter(exchange);
    }
    String redisKey = REDIS_PREFIX + rawKey.trim().toLowerCase(Locale.ROOT);
    return redisTemplate.opsForValue().get(redisKey)
        .flatMap(serialized -> deserialize(serialized)
            .map(cached -> writeCachedResponse(exchange, cached))
            .orElseGet(() -> chain.filter(exchange)))
        .switchIfEmpty(Mono.defer(() -> captureAndStore(redisKey, exchange, chain)));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 20;
  }

  private Mono<Void> captureAndStore(String redisKey, ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpResponse original = exchange.getResponse();
    DataBufferFactory bufferFactory = original.bufferFactory();
    ServerHttpResponseDecorator decorated = new ServerHttpResponseDecorator(original) {
      @Override
      public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
        Flux<? extends DataBuffer> flux = Flux.from(body);
        return DataBufferUtils.join(flux)
            .flatMap(buffer -> {
              byte[] bytes = new byte[buffer.readableByteCount()];
              buffer.read(bytes);
              DataBufferUtils.release(buffer);

              HttpStatusCode statusCode = getStatusCode();
              int status = statusCode != null ? statusCode.value() : HttpStatus.OK.value();

              Map<String, List<String>> headers = new LinkedHashMap<>();
              getHeaders().forEach((key, values) -> {
                if (!shouldPersistHeader(key) || CollectionUtils.isEmpty(values)) {
                  return;
                }
                headers.put(key, List.copyOf(values));
              });
              getHeaders().set("X-Idempotent-Replay", "false");

              IdempotentResponse snapshot = new IdempotentResponse(status, headers, bytes, Instant.now());

              Mono<Void> writeMono = super.writeWith(Mono.just(bufferFactory.wrap(bytes)));
              if (!shouldPersistStatus(status)) {
                return writeMono;
              }
              return store(redisKey, snapshot)
                  .onErrorResume(ex -> {
                    LOGGER.warn("Failed to persist idempotent response", ex);
                    return Mono.empty();
                  })
                  .then(writeMono);
            })
            .switchIfEmpty(super.writeWith(body));
      }

      @Override
      public Mono<Void> writeAndFlushWith(org.reactivestreams.Publisher<? extends org.reactivestreams.Publisher<? extends DataBuffer>> body) {
        return writeWith(Flux.from(body).flatMapSequential(publisher -> publisher));
      }
    };

    return chain.filter(exchange.mutate().response(decorated).build());
  }

  private Mono<Void> writeCachedResponse(ServerWebExchange exchange, IdempotentResponse cached) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.valueOf(cached.status()));
    HttpHeaders headers = response.getHeaders();
    headers.clear();
    cached.headers().forEach(headers::put);
    headers.set("X-Idempotent-Replay", "true");
    headers.setContentLength(cached.body().length);
    DataBuffer buffer = response.bufferFactory().wrap(cached.body());
    return response.writeWith(Mono.just(buffer));
  }

  private Mono<Void> store(String key, IdempotentResponse response) {
    try {
      String serialized = objectMapper.writeValueAsString(response);
      return redisTemplate.opsForValue()
          .set(key, serialized, CACHE_TTL)
          .then();
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  private Optional<IdempotentResponse> deserialize(String serialized) {
    try {
      return Optional.ofNullable(objectMapper.readValue(serialized, IdempotentResponse.class));
    } catch (Exception ex) {
      LOGGER.debug("Failed to decode cached idempotent response", ex);
      return Optional.empty();
    }
  }

  private boolean shouldPersistHeader(String key) {
    if (!StringUtils.hasText(key)) {
      return false;
    }
    String normalized = key.trim();
    return !HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(normalized)
        && !HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(normalized);
  }

  private boolean shouldPersistStatus(int status) {
    return status < 500;
  }

  private record IdempotentResponse(int status,
                                    Map<String, List<String>> headers,
                                    byte[] body,
                                    Instant storedAt) {
    IdempotentResponse {
      Objects.requireNonNull(headers, "headers");
      Objects.requireNonNull(body, "body");
      Objects.requireNonNull(storedAt, "storedAt");
    }
  }
}

