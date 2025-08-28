package com.shared.redis.starter.support;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Objects;

/**
 * Fixed-window rate limiter: allow N hits per window per key.
 */
public class RedisRateLimiter {

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis);
    }

    /**
     * @return true if call is allowed (count <= maxHits) within the window.
     */
    public boolean allow(String key, int maxHits, Duration window) {
        Long v = redis.opsForValue().increment(key);
        if (v != null && v == 1L) {
            // first hit: start the window
            redis.expire(key, window);
        }
        return v != null && v <= maxHits;
    }

    /**
     * @return current count in the active window (or 0 if none).
     */
    public long currentCount(String key) {
        String s = redis.opsForValue().get(key);
        return (s == null) ? 0L : Long.parseLong(s);
    }
}
