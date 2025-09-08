package com.ejada.redis.starter.support;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Simple distributed lock using SET NX PX + Lua for safe release/extend.
 * Keys are used as-is (prefix them at call sites if you use multi-tenancy).
 */
public class RedisLockService {

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
        // if current value equals token, delete
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "  return redis.call('del', KEYS[1]) " +
        "else " +
        "  return 0 " +
        "end", Long.class);

    private static final DefaultRedisScript<Long> EXTEND_SCRIPT = new DefaultRedisScript<>(
        // if owner, update ttl (pexpire)
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "  return redis.call('pexpire', KEYS[1], ARGV[2]) " +
        "else " +
        "  return 0 " +
        "end", Long.class);

    private final StringRedisTemplate redis;

    public RedisLockService(StringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis);
    }

    /**
     * Try to acquire a lock for a limited time. Returns a lock token if acquired, otherwise null.
     */
    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean ok = redis.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    /**
     * Release the lock if the token matches. Returns true if released.
     */
    public boolean unlock(String key, String token) {
        if (token == null) return false;
        List<String> keys = Collections.singletonList(key);
        Long res = redis.execute(RELEASE_SCRIPT, keys, token);
        return res != null && res == 1L;
    }

    /**
     * Extend the lock TTL if caller still owns it. Returns true if extended.
     */
    public boolean extend(String key, String token, Duration newTtl) {
        if (token == null || newTtl == null) return false;
        String ttshared = String.valueOf(newTtl.toMillis());
        Long res = redis.execute(EXTEND_SCRIPT, Collections.singletonList(key), token, ttshared);
        return res != null && res == 1L;
    }

    /**
     * Check whether the given token still owns the lock.
     */
    public boolean isOwner(String key, String token) {
        if (token == null) return false;
        String cur = redis.opsForValue().get(key);
        return token.equals(cur);
    }
}
