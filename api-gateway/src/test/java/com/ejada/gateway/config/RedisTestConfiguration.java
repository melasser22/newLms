package com.ejada.gateway.config;

import com.ejada.gateway.support.ReactiveRedisTestSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

@Configuration
public class RedisTestConfiguration {

  @Bean
  @Primary
  @ConditionalOnMissingBean(ReactiveStringRedisTemplate.class)
  ReactiveStringRedisTemplate testReactiveStringRedisTemplate() {
    return ReactiveRedisTestSupport.mockStringTemplate(ReactiveRedisTestSupport.newStore());
  }
}
