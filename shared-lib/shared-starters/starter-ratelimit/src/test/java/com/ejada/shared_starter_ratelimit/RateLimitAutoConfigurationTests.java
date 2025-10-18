package com.ejada.shared_starter_ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.Subscription;
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
        .withPropertyValues("shared.ratelimit.dynamic.enabled=false")
        .run(context -> {
          assertThat(context).hasSingleBean(TokenBucketLuaRateLimiter.class);
          assertThat(context).hasSingleBean(RateLimitService.class);
          assertThat(context).doesNotHaveBean(RateLimitSubscriptionListener.class);
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
      RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
      RedisConnection connection = mock(RedisConnection.class);
      Subscription subscription = mock(Subscription.class);
      when(factory.getConnection()).thenReturn(connection);
      AtomicBoolean subscribed = new AtomicBoolean(false);
      when(connection.isSubscribed()).thenAnswer(invocation -> subscribed.get());
      when(connection.getSubscription()).thenReturn(subscription);
      when(subscription.isAlive()).thenReturn(true);
      doAnswer(invocation -> {
        subscribed.set(true);
        return null;
      }).when(connection).subscribe(any(), any());
      return factory;
    }
  }
}
