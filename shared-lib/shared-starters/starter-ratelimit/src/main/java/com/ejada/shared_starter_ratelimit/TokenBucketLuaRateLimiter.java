package com.ejada.shared_starter_ratelimit;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Executes the token bucket Lua script against Redis.
 */
public class TokenBucketLuaRateLimiter {

  private static final String SCRIPT = "local rate = tonumber(ARGV[1])\n"
      + "local period = tonumber(ARGV[2])\n"
      + "local burst = tonumber(ARGV[3])\n"
      + "local now = tonumber(ARGV[4])\n"
      + "local requested = tonumber(ARGV[5])\n"
      + "local state = redis.call('HMGET', KEYS[1], 'tokens', 'ts')\n"
      + "local tokens = tonumber(state[1])\n"
      + "local ts = tonumber(state[2])\n"
      + "if tokens == nil then\n"
      + "  tokens = burst\n"
      + "  ts = now\n"
      + "end\n"
      + "if ts == nil then\n"
      + "  ts = now\n"
      + "end\n"
      + "local elapsed = now - ts\n"
      + "if elapsed < 0 then\n"
      + "  elapsed = 0\n"
      + "end\n"
      + "local refill = elapsed * (rate / period)\n"
      + "tokens = math.min(burst, tokens + refill)\n"
      + "local allowed = 0\n"
      + "local reason = 0\n"
      + "local retryAfter = 0\n"
      + "if tokens >= requested then\n"
      + "  allowed = 1\n"
      + "  tokens = tokens - requested\n"
      + "else\n"
      + "  local deficit = requested - tokens\n"
      + "  if burst <= 0 or rate <= 0 then\n"
      + "    reason = 2\n"
      + "  elseif tokens <= 0 then\n"
      + "    reason = 1\n"
      + "  else\n"
      + "    reason = 3\n"
      + "  end\n"
      + "  if rate > 0 then\n"
      + "    retryAfter = math.ceil((deficit / (rate / period)))\n"
      + "  end\n"
      + "end\n"
      + "redis.call('HMSET', KEYS[1], 'tokens', tokens, 'ts', now)\n"
      + "redis.call('PEXPIRE', KEYS[1], math.ceil(period))\n"
      + "return {allowed, tokens, reason, retryAfter}\n";

  private final StringRedisTemplate redisTemplate;
  private final RedisScript<List> redisScript;

  public TokenBucketLuaRateLimiter(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
    DefaultRedisScript<List> script = new DefaultRedisScript<>();
    script.setScriptText(SCRIPT);
    script.setResultType(List.class);
    this.redisScript = script;
  }

  public TokenBucketResponse consume(String key, RateLimitTier tier) {
    if (tier.requestsPerMinute() <= 0 || tier.burstCapacity() <= 0) {
      return new TokenBucketResponse(false, 0D, RateLimitReason.QUOTA_EXCEEDED, Duration.ZERO);
    }
    long now = System.currentTimeMillis();
    List<?> result = redisTemplate.execute(redisScript, Collections.singletonList(key),
        String.valueOf(tier.requestsPerMinute()),
        String.valueOf(tier.window().toMillis()),
        String.valueOf(tier.burstCapacity()),
        String.valueOf(now),
        "1");
    if (result == null || result.size() < 4) {
      return new TokenBucketResponse(true, tier.burstCapacity(), RateLimitReason.ALLOWED, Duration.ZERO);
    }
    boolean allowed = toNumber(result.get(0)) >= 1;
    double remaining = Math.max(0D, toDouble(result.get(1)));
    RateLimitReason reason = mapReason(toNumber(result.get(2)));
    Duration retryAfter = Duration.ofMillis(Math.max(0L, toNumber(result.get(3))));
    if (allowed) {
      return new TokenBucketResponse(true, remaining, RateLimitReason.ALLOWED, Duration.ZERO);
    }
    return new TokenBucketResponse(false, remaining, reason, retryAfter);
  }

  private long toNumber(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    return Long.parseLong(String.valueOf(value));
  }

  private double toDouble(Object value) {
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    return Double.parseDouble(String.valueOf(value));
  }

  private RateLimitReason mapReason(long code) {
    return switch ((int) code) {
      case 1 -> RateLimitReason.RATE_LIMIT_HIT;
      case 2 -> RateLimitReason.QUOTA_EXCEEDED;
      case 3 -> RateLimitReason.BURST_CAPACITY_FULL;
      default -> RateLimitReason.ALLOWED;
    };
  }

  public record TokenBucketResponse(boolean allowed, double remainingTokens, RateLimitReason reason,
                                    Duration retryAfter) {
  }
}
