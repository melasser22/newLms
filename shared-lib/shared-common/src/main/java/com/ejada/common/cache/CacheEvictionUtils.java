package com.ejada.common.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/** Common helpers for evicting Spring cache entries from multiple services. */
public final class CacheEvictionUtils {

    private CacheEvictionUtils() {
        // utility
    }

    public static void evict(final CacheManager cacheManager, final String cacheName, final Object key) {
        if (cacheManager == null || cacheName == null) {
            return;
        }
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null && key != null && !isBlankCharSequence(key)) {
            cache.evict(key);
        }
    }

    public static void evict(final CacheManager cacheManager, final String cacheName, final Object... keys) {
        if (cacheManager == null || cacheName == null || keys == null) {
            return;
        }
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return;
        }
        for (Object key : keys) {
            if (key != null && !isBlankCharSequence(key)) {
                cache.evict(key);
            }
        }
    }

    private static boolean isBlankCharSequence(final Object key) {
        if (key instanceof CharSequence sequence) {
            return sequence.toString().trim().isEmpty();
        }
        return false;
    }
}
