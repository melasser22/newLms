package com.lms.setup.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Default cache configuration with enhanced performance
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues()
                .prefixCacheNameWith("lms:")
                .computePrefixWith(cacheName -> "lms:" + cacheName + ":");

        // Specific cache configurations with optimized TTLs
        cacheConfigurations.put("countries", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("countries:active", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put("cities", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("cities:active", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put("lookups", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("lookups:active", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put("resources", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("resources:active", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put("system-parameters", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("system-parameters:active", defaultConfig.entryTtl(Duration.ofHours(2)));

        // Short-lived caches for frequently changing data
        cacheConfigurations.put("user-sessions", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("rate-limits", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
