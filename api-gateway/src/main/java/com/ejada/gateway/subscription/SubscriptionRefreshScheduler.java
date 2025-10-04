package com.ejada.gateway.subscription;

import com.ejada.gateway.config.SubscriptionValidationProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Periodically refresh subscription cache entries before they expire to minimise cache misses.
 */
@Component
public class SubscriptionRefreshScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionRefreshScheduler.class);

  private final SubscriptionValidationProperties properties;
  private final SubscriptionCacheService cacheService;
  private final ReactiveStringRedisTemplate redisTemplate;

  public SubscriptionRefreshScheduler(
      SubscriptionValidationProperties properties,
      SubscriptionCacheService cacheService,
      ReactiveStringRedisTemplate redisTemplate) {
    this.properties = Objects.requireNonNull(properties, "properties");
    this.cacheService = Objects.requireNonNull(cacheService, "cacheService");
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
  }

  @Scheduled(fixedDelayString = "PT1M")
  public void refreshExpiringSubscriptions() {
    if (!properties.isEnabled()) {
      return;
    }
    Duration ttl = properties.getCacheTtl();
    if (ttl == null || ttl.isZero() || ttl.isNegative()) {
      return;
    }
    Duration refreshThreshold = ttl.multipliedBy(8).dividedBy(10);
    cacheService.readAllCached()
        .flatMapMany(cache -> Flux.fromIterable(cache.entrySet()))
        .flatMap(entry -> refreshIfNecessary(entry, refreshThreshold))
        .onErrorContinue((ex, key) -> LOGGER.debug("Subscription refresh error for {}", key, ex))
        .subscribe();
  }

  private Mono<Void> refreshIfNecessary(Map.Entry<String, SubscriptionRecord> entry, Duration threshold) {
    SubscriptionRecord record = entry.getValue();
    if (record == null) {
      return Mono.empty();
    }
    Duration age = Duration.between(record.fetchedAt(), Instant.now());
    if (age.compareTo(threshold) < 0) {
      return Mono.empty();
    }
    String tenantId = tenantIdFromKey(entry.getKey());
    if (tenantId == null) {
      return Mono.empty();
    }
    return acquireLock(tenantId)
        .filter(Boolean::booleanValue)
        .flatMap(acquired -> Mono.usingWhen(
            Mono.just(tenantId),
            ignored -> cacheService.fetchAndCache(ignored)
                .doOnSuccess(updated -> LOGGER.debug("Refreshed subscription cache for tenant {}", ignored))
                .then(),
            this::releaseLock,
            (ignored, ex) -> releaseLock(ignored),
            this::releaseLock));
  }

  private Mono<Boolean> acquireLock(String tenantId) {
    Duration lockTtl = Duration.ofMinutes(1);
    return redisTemplate.opsForValue()
        .setIfAbsent(refreshLockKey(tenantId), "1", lockTtl)
        .defaultIfEmpty(Boolean.FALSE);
  }

  private Mono<Void> releaseLock(String tenantId) {
    return redisTemplate.delete(refreshLockKey(tenantId)).then();
  }

  private String refreshLockKey(String tenantId) {
    return "gateway:subscription:refresh-lock:" + tenantId;
  }

  private String tenantIdFromKey(String key) {
    String prefix = properties.getCachePrefix();
    if (!key.startsWith(prefix)) {
      return null;
    }
    return key.substring(prefix.length());
  }
}
