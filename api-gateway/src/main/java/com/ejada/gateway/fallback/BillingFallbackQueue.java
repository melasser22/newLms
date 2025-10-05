package com.ejada.gateway.fallback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Persists usage events when the billing service circuit breaker opens so they
 * can be replayed once the downstream dependency recovers.
 */
@Component
public class BillingFallbackQueue {

  private static final Logger LOGGER = LoggerFactory.getLogger(BillingFallbackQueue.class);
  private static final String DEFAULT_QUEUE_KEY = "gateway:billing:fallback";

  private final ReactiveStringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final String queueKey;

  @Autowired
  public BillingFallbackQueue(ReactiveStringRedisTemplate redisTemplate,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper) {
    this(redisTemplate, primaryObjectMapper, fallbackObjectMapper, DEFAULT_QUEUE_KEY);
  }

  public BillingFallbackQueue(ReactiveStringRedisTemplate redisTemplate,
      ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper,
      String queueKey) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
    ObjectMapper mapper = (primaryObjectMapper != null) ? primaryObjectMapper.getIfAvailable() : null;
    if (mapper == null && fallbackObjectMapper != null) {
      mapper = fallbackObjectMapper.getIfAvailable();
    }
    if (mapper == null) {
      mapper = new ObjectMapper();
    }
    this.objectMapper = mapper;
    this.queueKey = StringUtils.hasText(queueKey) ? queueKey.trim() : DEFAULT_QUEUE_KEY;
  }

  public Mono<String> enqueue(String tenantId, ServerHttpRequest request) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("tenantId", StringUtils.hasText(tenantId) ? tenantId : "unknown");
    String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
    payload.put("method", method);
    URI uri = request.getURI();
    payload.put("path", uri.getPath());
    payload.put("query", uri.getQuery());
    payload.put("timestamp", Instant.now().toString());
    payload.put("headers", request.getHeaders().toSingleValueMap());
    String serialised;
    try {
      serialised = objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      LOGGER.warn("Failed to serialise billing fallback payload", ex);
      serialised = payload.toString();
    }
    return redisTemplate.opsForList().leftPush(queueKey, serialised)
        .doOnError(ex -> LOGGER.warn("Failed to queue billing fallback event", ex))
        .map(ignored -> queueKey);
  }

  public String queueKey() {
    return queueKey;
  }
}
