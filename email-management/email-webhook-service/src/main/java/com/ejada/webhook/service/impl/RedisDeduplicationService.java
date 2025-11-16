package com.ejada.webhook.service.impl;

import com.ejada.webhook.service.DeduplicationService;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisDeduplicationService implements DeduplicationService {

  private final StringRedisTemplate redisTemplate;

  public RedisDeduplicationService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public boolean seen(String messageId) {
    Boolean stored =
        redisTemplate.opsForValue().setIfAbsent("webhook:event:" + messageId, "1", Duration.ofHours(1));
    return !Boolean.TRUE.equals(stored);
  }
}
