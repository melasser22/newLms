package com.ejada.shared_starter_ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

class RateLimitAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(RateLimitAutoConfiguration.class));

  @Test
  void backsOffWhenRedisTemplateIsNotOnClasspath() {
    contextRunner
        .withClassLoader(new FilteredClassLoader(StringRedisTemplate.class))
        .run(context -> assertThat(context).doesNotHaveBean(TokenBucketLuaRateLimiter.class));
  }

  @Test
  void createsRedisBackedBeansWhenTemplatePresent() {
    contextRunner
        .withUserConfiguration(RedisTemplateConfiguration.class)
        .run(context -> {
          assertThat(context).hasSingleBean(TokenBucketLuaRateLimiter.class);
          assertThat(context).hasSingleBean(RateLimitService.class);
          assertThat(context).hasSingleBean(RateLimitSubscriptionListener.class);
        });
  }

  @Configuration(proxyBeanMethods = false)
  static class RedisTemplateConfiguration {

    @Bean
    StringRedisTemplate stringRedisTemplate() {
      return mock(StringRedisTemplate.class);
    }

    @Bean
    RedisConnectionFactory redisConnectionFactory() {
      return mock(RedisConnectionFactory.class);
    }
  }
}
