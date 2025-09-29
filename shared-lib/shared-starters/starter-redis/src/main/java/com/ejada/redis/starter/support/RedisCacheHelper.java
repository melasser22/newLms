package com.ejada.redis.starter.support;

import com.ejada.redis.starter.config.KeyPrefixStrategy;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Small helper around {@link RedisTemplate} that automatically applies the environment
 * specific key prefix and exposes Optional based convenience methods. Centralising the
 * prefix handling keeps the domain services focused on their business logic and removes
 * a noticeable amount of boilerplate.
 */
@Component
public class RedisCacheHelper {

    private final RedisTemplate<String, Object> redisTemplate;
    private final KeyPrefixStrategy keyPrefixStrategy;

    public RedisCacheHelper(
            RedisTemplate<String, Object> redisTemplate,
            KeyPrefixStrategy keyPrefixStrategy) {
        this.redisTemplate = redisTemplate;
        this.keyPrefixStrategy = keyPrefixStrategy;
    }

    /**
     * Resolves the full key including the configured prefix for the provided suffix.
     */
    public String key(String suffix) {
        return keyPrefixStrategy.resolvePrefix() + suffix;
    }

    /**
     * Stores the value under the given key suffix.
     */
    public void set(String suffix, Object value) {
        redisTemplate.opsForValue().set(key(suffix), value);
    }

    /**
     * Deletes the entry for the given key suffix if it exists.
     */
    public void delete(String suffix) {
        redisTemplate.delete(key(suffix));
    }

    /**
     * Fetches the cached value if present. The caller is responsible for performing any
     * required casting when working with parameterised types such as {@code List<RoleDto>}.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String suffix) {
        Object value = redisTemplate.opsForValue().get(key(suffix));
        return Optional.ofNullable((T) value);
    }
}
