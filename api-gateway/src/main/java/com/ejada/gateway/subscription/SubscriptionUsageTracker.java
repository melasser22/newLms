package com.ejada.gateway.subscription;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Tracks feature usage per tenant using Redis counters to enforce subscription quotas.
 */
@Component
public class SubscriptionUsageTracker {

  private static final String USAGE_KEY_FORMAT = "gateway:usage:%s:%s:%s";

  private final ReactiveStringRedisTemplate redisTemplate;

  public SubscriptionUsageTracker(ReactiveStringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public Mono<UsageCheck> recordUsage(String tenantId, String feature, SubscriptionRecord record) {
    if (!StringUtils.hasText(feature)) {
      return Mono.just(UsageCheck.unlimited(feature));
    }
    return record.allocationFor(feature)
        .map(allocation -> {
          if (!allocation.enabled() || allocation.unlimited()) {
            return Mono.just(UsageCheck.unlimited(feature));
          }
          String key = usageKey(tenantId, allocation.feature());
          return redisTemplate.opsForValue()
              .increment(key)
              .flatMap(count -> ensureExpiry(key)
                  .thenReturn(count))
              .map(count -> UsageCheck.checked(feature, allocation.limit(), count));
        })
        .orElse(Mono.just(UsageCheck.unavailable(feature)));
  }

  private Mono<Boolean> ensureExpiry(String key) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate tomorrow = today.plusDays(1);
    Duration ttl = Duration.between(instantAtStartOfDay(today), instantAtStartOfDay(tomorrow));
    return redisTemplate.expire(key, ttl);
  }

  private java.time.Instant instantAtStartOfDay(LocalDate date) {
    return date.atStartOfDay().toInstant(ZoneOffset.UTC);
  }

  private String usageKey(String tenantId, String feature) {
    String safeTenant = StringUtils.hasText(tenantId) ? tenantId : "unknown";
    String safeFeature = feature != null ? feature.toLowerCase(Locale.ROOT) : "unknown";
    return String.format(USAGE_KEY_FORMAT, safeTenant, safeFeature, LocalDate.now(ZoneOffset.UTC));
  }

  public record UsageCheck(String feature, boolean exceeded, Long limit, long usage) {

    static UsageCheck unlimited(String feature) {
      return new UsageCheck(feature, false, null, 0L);
    }

    static UsageCheck unavailable(String feature) {
      return new UsageCheck(feature, false, null, 0L);
    }

    static UsageCheck checked(String feature, Long limit, long usage) {
      boolean exceeded = limit != null && limit >= 0 && usage > limit;
      return new UsageCheck(feature, exceeded, limit, usage);
    }
  }
}
