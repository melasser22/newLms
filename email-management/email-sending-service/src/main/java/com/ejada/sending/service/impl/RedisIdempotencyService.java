package com.ejada.sending.service.impl;

import com.ejada.sending.service.IdempotencyService;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisIdempotencyService implements IdempotencyService {

  private final StringRedisTemplate redisTemplate;

  public RedisIdempotencyService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public boolean register(String tenantId, String key, String value) {
    String redisKey = "email:idempotency:" + tenantId + ':' + key;
    Boolean stored = redisTemplate.opsForValue().setIfAbsent(redisKey, value, Duration.ofHours(2));
    return Boolean.TRUE.equals(stored);
  }
}
