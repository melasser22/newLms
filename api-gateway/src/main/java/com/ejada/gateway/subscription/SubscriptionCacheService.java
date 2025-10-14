package com.ejada.gateway.subscription;

import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.SubscriptionValidationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Encapsulates subscription fetching and caching interactions with Redis.
 */
@Component
public class SubscriptionCacheService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionCacheService.class);

  private static final ParameterizedTypeReference<BaseResponse<SubscriptionPayload>> RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {
      };

  private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(5);

  private final SubscriptionValidationProperties properties;
  private final ReactiveStringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final WebClient webClient;
  private final AtomicLong cacheHits = new AtomicLong();
  private final AtomicLong cacheMisses = new AtomicLong();
  private final Cache<String, SubscriptionRecord> localCache;

  public SubscriptionCacheService(
      SubscriptionValidationProperties properties,
      ReactiveStringRedisTemplate redisTemplate,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper,
      WebClient.Builder webClientBuilder,
      MeterRegistry meterRegistry) {
    this.properties = properties;
    this.redisTemplate = redisTemplate;
    ObjectMapper mapper = (primaryObjectMapper != null) ? primaryObjectMapper.getIfAvailable() : null;
    if (mapper == null) {
      mapper = (fallbackObjectMapper != null) ? fallbackObjectMapper.getIfAvailable() : null;
    }
    this.objectMapper = mapper;
    this.webClient = webClientBuilder.clone().build();
    this.localCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(30))
        .maximumSize(20_000)
        .build();
    Gauge.builder("gateway.subscription.validation.cache_hit_rate", this,
            SubscriptionCacheService::calculateHitRate)
        .description("Cache hit ratio for subscription validation lookups")
        .register(meterRegistry);
  }

  public Mono<Optional<SubscriptionRecord>> getCached(String tenantId) {
    String key = cacheKey(tenantId);
    SubscriptionRecord local = localCache.getIfPresent(key);
    if (local != null) {
      recordCacheHit();
      return Mono.just(Optional.of(local));
    }
    if (redisTemplate == null) {
      recordCacheMiss();
      return Mono.just(Optional.empty());
    }
    return redisTemplate.opsForValue().get(key)
        .flatMap(json -> decode(json)
            .map(record -> {
              recordCacheHit();
              localCache.put(key, record);
              return Mono.just(record);
            })
            .orElseGet(() -> {
              recordCacheMiss();
              return Mono.empty();
            }))
        .map(Optional::of)
        .switchIfEmpty(Mono.fromCallable(() -> {
          recordCacheMiss();
          return Optional.empty();
        }));
  }

  public Mono<SubscriptionRecord> fetch(String tenantId) {
    return webClient.get()
        .uri(properties.getValidationUri(), tenantId)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(RESPONSE_TYPE)
        .map(this::extractPayload)
        .doOnNext(record -> LOGGER.debug("Fetched subscription for tenant {} -> {}", tenantId,
            record.status()))
        .switchIfEmpty(Mono.just(SubscriptionRecord.inactive()));
  }

  double calculateHitRate() {
    long hits = cacheHits.get();
    long misses = cacheMisses.get();
    long total = hits + misses;
    if (total == 0) {
      return 0.0d;
    }
    return (double) hits / total;
  }

  private void recordCacheHit() {
    cacheHits.incrementAndGet();
  }

  private void recordCacheMiss() {
    cacheMisses.incrementAndGet();
  }

  public Mono<SubscriptionRecord> fetchAndCache(String tenantId) {
    String key = cacheKey(tenantId);
    return fetch(tenantId).flatMap(record -> cacheByKey(key, record).thenReturn(record));
  }

  public Mono<Void> cacheTenant(String tenantId, SubscriptionRecord record) {
    return cacheByKey(cacheKey(tenantId), record);
  }

  public Mono<Void> cache(String cacheKey, SubscriptionRecord record) {
    return cacheByKey(cacheKey, record);
  }

  private Mono<Void> cacheByKey(String cacheKey, SubscriptionRecord record) {
    try {
      String value = objectMapper.writeValueAsString(record);
      Duration configuredTtl = properties.getCacheTtl();
      Duration ttl = (configuredTtl == null || configuredTtl.isNegative())
          ? DEFAULT_CACHE_TTL
          : configuredTtl;
      Mono<Boolean> writeOperation;
      if (redisTemplate == null) {
        localCache.put(cacheKey, record);
        return Mono.empty();
      }
      if (ttl.isZero()) {
        writeOperation = redisTemplate.opsForValue().set(cacheKey, value);
      } else {
        writeOperation = redisTemplate.opsForValue().set(cacheKey, value, ttl);
      }
      localCache.put(cacheKey, record);
      return writeOperation.then();
    } catch (JsonProcessingException e) {
      LOGGER.debug("Failed to serialise subscription record for cache", e);
      return Mono.empty();
    }
  }

  public Optional<SubscriptionRecord> decode(String json) {
    try {
      return Optional.ofNullable(objectMapper.readValue(json, SubscriptionRecord.class));
    } catch (Exception ex) {
      LOGGER.debug("Failed to decode cached subscription payload", ex);
      return Optional.empty();
    }
  }

  public String cacheKey(String tenantId) {
    return properties.cacheKey(tenantId, null);
  }

  public Mono<Map<String, SubscriptionRecord>> readAllCached() {
    String pattern = properties.getCachePrefix() + "*";
    if (redisTemplate == null) {
      return Mono.just(new HashMap<>(localCache.asMap()));
    }
    return redisTemplate.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
            .match(pattern)
            .count(200)
            .build())
        .flatMap(key -> redisTemplate.opsForValue().get(key)
            .flatMap(json -> decode(json)
                .map(record -> Mono.just(Map.entry(key, record)))
                .orElseGet(Mono::empty)))
        .collectMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  public Mono<Void> evictTenant(String tenantId) {
    if (!StringUtils.hasText(tenantId)) {
      return Mono.empty();
    }
    String key = cacheKey(tenantId);
    localCache.invalidate(key);
    if (redisTemplate == null) {
      return Mono.empty();
    }
    return redisTemplate.delete(key).then();
  }

  private SubscriptionRecord extractPayload(BaseResponse<SubscriptionPayload> response) {
    if (response == null) {
      return SubscriptionRecord.inactive();
    }
    SubscriptionPayload payload = response.getData();
    if (payload == null) {
      return SubscriptionRecord.inactive();
    }
    boolean active = payload.active != null ? payload.active : "ACTIVE".equalsIgnoreCase(payload.status);
    Set<String> features = (payload.features != null) ? Set.copyOf(payload.features) : Set.of();
    Map<String, SubscriptionRecord.FeatureAllocation> allocations = new LinkedHashMap<>();
    if (!CollectionUtils.isEmpty(payload.featureAllocations)) {
      payload.featureAllocations.forEach((name, details) -> {
        if (!StringUtils.hasText(name)) {
          return;
        }
        boolean enabled = details.enabled != null ? details.enabled : features.contains(name);
        Long limit = details.limit;
        allocations.put(name, new SubscriptionRecord.FeatureAllocation(name, enabled, limit));
      });
    } else if (!features.isEmpty()) {
      features.forEach(feature -> allocations.put(feature,
          new SubscriptionRecord.FeatureAllocation(feature, true, null)));
    }
    return SubscriptionRecord.of(active, features, payload.expiresAt, allocations, payload.upgradeUrl);
  }

  private static final class SubscriptionPayload {

    private Boolean active;
    private String status;
    private java.util.List<String> features;
    private Instant expiresAt;
    private Map<String, FeatureAllocationPayload> featureAllocations = new HashMap<>();
    private String upgradeUrl;

  }

  private static final class FeatureAllocationPayload {

    private Boolean enabled;
    private Long limit;

  }
}
