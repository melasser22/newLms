package com.shared.redis.starter.support;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Idempotency helper: registers a key for a TTL; rejects repeats.
 */
public class RedisIdempotencyService {

    private final StringRedisTemplate redis;

    public RedisIdempotencyService(StringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis);
    }

    /**
     * Register an idempotency key once. Returns true if accepted (first time).
     */
    public boolean registerOnce(String key, Duration ttl) {
        Boolean ok = redis.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }

    /**
     * Execute supplier only if key not seen within TTL. Returns null if duplicate.
     */
    public <T> T doOnce(String key, Duration ttl, Supplier<T> work) {
        if (registerOnce(key, ttl)) {
            return work.get();
        }
        return null; // duplicate request
    }
}
