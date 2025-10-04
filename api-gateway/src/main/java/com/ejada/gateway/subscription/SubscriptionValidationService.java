package com.ejada.gateway.subscription;

import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.SubscriptionValidationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Shared service that encapsulates subscription fetching and caching logic.
 */
@Service
public class SubscriptionValidationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionValidationService.class);
  private static final ParameterizedTypeReference<BaseResponse<SubscriptionPayload>> RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {};

  private final SubscriptionValidationProperties properties;
  private final WebClient webClient;
  private final ObjectMapper objectMapper;
  private final ReactiveStringRedisTemplate redisTemplate;

  public SubscriptionValidationService(SubscriptionValidationProperties properties,
      WebClient.Builder webClientBuilder,
      ObjectMapper objectMapper,
      ReactiveStringRedisTemplate redisTemplate) {
    this.properties = properties;
    this.webClient = webClientBuilder.build();
    this.objectMapper = objectMapper;
    this.redisTemplate = redisTemplate;
  }

  public Mono<SubscriptionRecord> getSubscription(String tenantId, @Nullable String feature) {
    String cacheKey = properties.cacheKey(tenantId, feature);
    return redisTemplate.opsForValue().get(cacheKey)
        .flatMap(value -> Mono.justOrEmpty(decode(value)))
        .switchIfEmpty(fetchSubscription(tenantId)
            .flatMap(record -> cache(cacheKey, record).thenReturn(record)));
  }

  public Mono<Void> warmupTenant(String tenantId) {
    if (!StringUtils.hasText(tenantId)) {
      return Mono.empty();
    }
    return getSubscription(tenantId, null)
        .doOnSuccess(record -> LOGGER.info("Warm cache for tenant {} (status: {})", tenantId, record.status()))
        .onErrorResume(ex -> {
          LOGGER.warn("Failed to warm cache for tenant {}", tenantId, ex);
          return Mono.empty();
        })
        .then();
  }

  private Mono<SubscriptionRecord> fetchSubscription(String tenantId) {
    String uri = properties.getValidationUri().replace("{tenantId}", tenantId);
    return webClient.get().uri(uri)
        .retrieve()
        .bodyToMono(RESPONSE_TYPE)
        .map(this::extractPayload)
        .switchIfEmpty(Mono.just(SubscriptionRecord.inactive()))
        .onErrorResume(ex -> {
          LOGGER.warn("Subscription lookup failed for tenant {}", tenantId, ex);
          return Mono.just(SubscriptionRecord.inactive());
        });
  }

  private SubscriptionRecord extractPayload(BaseResponse<SubscriptionPayload> response) {
    if (response == null || response.getData() == null) {
      return SubscriptionRecord.inactive();
    }
    SubscriptionPayload payload = response.getData();
    Set<String> features = (payload.features != null) ? payload.features : Set.of();
    boolean active = Boolean.TRUE.equals(payload.active) && !isExpired(payload.expiresAt);
    return SubscriptionRecord.of(active, features, payload.expiresAt);
  }

  private boolean isExpired(Instant expiresAt) {
    return expiresAt != null && Instant.now().isAfter(expiresAt);
  }

  private Optional<SubscriptionRecord> decode(String json) {
    try {
      return Optional.ofNullable(objectMapper.readValue(json, SubscriptionRecord.class));
    } catch (JsonProcessingException ex) {
      LOGGER.debug("Failed to decode cached subscription payload", ex);
      return Optional.empty();
    }
  }

  private Mono<Void> cache(String cacheKey, SubscriptionRecord record) {
    Duration ttl = properties.getCacheTtl();
    try {
      String payload = objectMapper.writeValueAsString(record);
      return redisTemplate.opsForValue().set(cacheKey, payload, ttl).then();
    } catch (JsonProcessingException e) {
      LOGGER.debug("Failed to serialise subscription record for cache", e);
      return Mono.empty();
    }
  }

  private record SubscriptionPayload(Boolean active, Set<String> features, Instant expiresAt) {
  }
}
