package com.ejada.sec.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.cache.interceptor.KeyGenerator;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Configures caching for permission resolution with monitoring support.
 */
@Configuration
@EnableCaching
@Slf4j
@RequiredArgsConstructor
public class PermissionCacheConfig {

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    @Value("${sec.permissions.cache.ttl-minutes:5}")
    private int cacheTtlMinutes;

    @Value("${sec.permissions.cache.max-size:10000}")
    private long cacheMaxSize;

    @Value("${sec.permissions.cache.enabled:true}")
    private boolean cacheEnabled;

    /**
     * Creates the primary cache manager backed by Caffeine.
     */
    @Bean
    @Primary
    public CacheManager permissionCacheManager() {
        if (!cacheEnabled) {
            log.warn("Permission caching is DISABLED - expect poor performance");
            return new NoOpCacheManager();
        }

        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
            buildCache("user-permissions", meterRegistry),
            buildCache("user-permission-map", meterRegistry),
            buildCache("resource-permissions", meterRegistry),
            buildCache("role-privileges", meterRegistry)
        ));
        cacheManager.initializeCaches();

        log.info("Permission cache initialized: TTL={}min, MaxSize={}, Enabled={}",
            cacheTtlMinutes, cacheMaxSize, cacheEnabled);

        return cacheManager;
    }

    private CaffeineCache buildCache(String name, MeterRegistry meterRegistry) {
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
            Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(cacheTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();

        if (meterRegistry != null) {
            CaffeineCacheMetrics.monitor(meterRegistry, nativeCache, name);
        }

        log.info("Created cache '{}' with TTL={}min, MaxSize={}", name, cacheTtlMinutes, cacheMaxSize);
        return new CaffeineCache(name, nativeCache, true);
    }

    /**
     * Custom key generator suitable for multi-parameter methods.
     */
    @Bean
    public KeyGenerator permissionKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder key = new StringBuilder();
            for (Object param : params) {
                key.append(param != null ? param : "null").append(":");
            }
            return key.toString();
        };
    }
}
