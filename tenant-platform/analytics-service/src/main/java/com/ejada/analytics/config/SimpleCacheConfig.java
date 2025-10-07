package com.ejada.analytics.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMissingBean(CacheManager.class)
public class SimpleCacheConfig {

  @Bean
  public CacheManager fallbackCacheManager() {
    return new ConcurrentMapCacheManager();
  }
}
