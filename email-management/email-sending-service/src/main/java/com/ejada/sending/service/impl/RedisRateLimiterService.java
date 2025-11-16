package com.ejada.sending.service.impl;

import com.ejada.sending.config.RateLimitProperties;
import com.ejada.sending.service.RateLimiterService;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisRateLimiterService implements RateLimiterService {

  private final StringRedisTemplate redisTemplate;
  private final RateLimitProperties properties;

  public RedisRateLimiterService(
      StringRedisTemplate redisTemplate, RateLimitProperties properties) {
    this.redisTemplate = redisTemplate;
    this.properties = properties;
  }

  @Override
  public boolean tryConsume(String tenantId) {
    String redisKey = "email:rate:" + tenantId;
    Long tokens = redisTemplate.opsForValue().increment(redisKey, -1);
    if (tokens == null) {
      redisTemplate
          .opsForValue()
          .set(redisKey, String.valueOf(properties.getCapacity() - 1), Duration.ofMinutes(1));
      return true;
    }
    if (tokens >= 0) {
      return true;
    }
    redisTemplate.opsForValue().set(redisKey, String.valueOf(properties.getCapacity()), Duration.ofMinutes(1));
    return false;
  }
}
