package com.ejada.template.service.support;

import com.ejada.template.config.RateLimitProperties;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisIdempotencyService {

  private final StringRedisTemplate redisTemplate;
  private final RateLimitProperties properties;

  public Optional<Long> findSendId(String tenantId, String key) {
    String value = redisTemplate.opsForValue().get(idempotencyKey(normalizeTenant(tenantId), key));
    if (value == null) {
      return Optional.empty();
    }
    return Optional.of(Long.parseLong(value));
  }

  public void storeSendId(String tenantId, String key, Long sendId) {
    Duration ttl = properties.idempotencyTtl();
    redisTemplate
        .opsForValue()
        .set(idempotencyKey(normalizeTenant(tenantId), key), String.valueOf(sendId), ttl);
  }

  private String idempotencyKey(String tenantId, String key) {
    return "idem:" + tenantId + ":" + key;
  }

  private String normalizeTenant(String tenantId) {
    return (tenantId == null || tenantId.isBlank()) ? "global" : tenantId;
  }
}
