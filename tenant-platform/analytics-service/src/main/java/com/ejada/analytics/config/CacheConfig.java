package com.ejada.analytics.config;

import java.time.Duration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class CacheConfig {

  private static final Duration CACHE_TTL = Duration.ofHours(1);

  @Bean
  public RedisCacheConfiguration redisCacheConfiguration() {
    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(CACHE_TTL)
        .disableCachingNullValues();
  }

  @Bean
  public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
    return builder -> builder.cacheDefaults(redisCacheConfiguration());
  }
}
