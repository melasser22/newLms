package com.ejada.email.sending.service.impl;

import com.ejada.email.sending.service.IdempotencyService;
import java.time.Duration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Redis template is managed by Spring")
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
