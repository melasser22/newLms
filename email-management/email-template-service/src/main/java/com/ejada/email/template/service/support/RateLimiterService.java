package com.ejada.email.template.service.support;

import com.ejada.email.template.config.RateLimitProperties;
import com.ejada.email.template.exception.RateLimitExceededException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RateLimiterService {

  private final StringRedisTemplate redisTemplate;
  private final RateLimitProperties properties;

  public void validateQuota(String tenantId, String action) {
    String key = rateKey(normalizeTenant(tenantId), action);
    Long current = redisTemplate.opsForValue().increment(key);
    Duration window = properties.window();
    if (current != null && current == 1L) {
      redisTemplate.expire(key, window);
    }
    if (current != null && current > properties.allowedTokens()) {
      throw new RateLimitExceededException(tenantId, action);
    }
  }

  private String rateKey(String tenantId, String action) {
    return "rate:" + tenantId + ":" + action;
  }

  private String normalizeTenant(String tenantId) {
    return (tenantId == null || tenantId.isBlank()) ? "global" : tenantId;
  }
}
