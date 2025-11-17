package com.ejada.template.service.support;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebhookDeduplicationService {

  private final StringRedisTemplate redisTemplate;

  public boolean isDuplicate(String tenantId, String eventId, Duration ttl) {
    String key = "webhook:" + tenantId + ":" + eventId;
    Boolean created = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
    return Boolean.FALSE.equals(created);
  }
}
