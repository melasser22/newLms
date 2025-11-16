package com.ejada.sending.service.impl;

import com.ejada.sending.config.RateLimitProperties;
import com.ejada.sending.service.RateLimiterService;
import java.time.Instant;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisRateLimiterService implements RateLimiterService {

  private final StringRedisTemplate redisTemplate;
  private final RateLimitProperties properties;
  private final DefaultRedisScript<Long> tokenScript;

  public RedisRateLimiterService(
      StringRedisTemplate redisTemplate, RateLimitProperties properties) {
    this.redisTemplate = redisTemplate;
    this.properties = properties;
    this.tokenScript =
        new DefaultRedisScript<>(
            "local tokens_key = KEYS[1]\n"
                + "local timestamp_key = KEYS[2]\n"
                + "local capacity = tonumber(ARGV[1])\n"
                + "local refill_per_minute = tonumber(ARGV[2])\n"
                + "local now = tonumber(ARGV[3])\n"
                + "local last_refill = tonumber(redis.call('get', timestamp_key) or now)\n"
                + "local tokens = tonumber(redis.call('get', tokens_key) or capacity)\n"
                + "local elapsed = now - last_refill\n"
                + "local refill = (elapsed * refill_per_minute) / 60\n"
                + "tokens = math.min(capacity, tokens + refill)\n"
                + "if tokens < 1 then\n"
                + "  redis.call('set', tokens_key, tokens)\n"
                + "  redis.call('set', timestamp_key, now)\n"
                + "  return 0\n"
                + "end\n"
                + "tokens = tokens - 1\n"
                + "redis.call('set', tokens_key, tokens)\n"
                + "redis.call('set', timestamp_key, now)\n"
                + "return 1",
            Long.class);
  }

  @Override
  public boolean tryConsume(String tenantId) {
    List<String> keys = List.of("email:rate:tokens:" + tenantId, "email:rate:ts:" + tenantId);
    Long allowed =
        redisTemplate.execute(
            tokenScript,
            keys,
            String.valueOf(properties.getCapacity()),
            String.valueOf(properties.getRefillPerMinute()),
            String.valueOf(Instant.now().getEpochSecond()));
    return Long.valueOf(1L).equals(allowed);
  }
}
