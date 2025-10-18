package com.ejada.shared_starter_ratelimit;

import com.ejada.audit.starter.api.AuditService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@EnableConfigurationProperties(RateLimitProps.class)
public class RateLimitAutoConfiguration {

  @Bean
  public TenantRateLimitRegistry tenantRateLimitRegistry(RateLimitProps props) {
    return new TenantRateLimitRegistry(props);
  }

  @Bean
  public RateLimitKeyGenerator rateLimitKeyGenerator() {
    return new RateLimitKeyGenerator();
  }

  @Bean
  public RateLimitMetricsRecorder rateLimitMetricsRecorder(ObjectProvider<MeterRegistry> registry) {
    return new RateLimitMetricsRecorder(registry.getIfAvailable());
  }

  @Bean
  public RateLimitBypassEvaluator rateLimitBypassEvaluator(RateLimitProps props) {
    props.applyDefaults();
    return new RateLimitBypassEvaluator(props.getBypass());
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(StringRedisTemplate.class)
  static class RedisRateLimitConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    TokenBucketLuaRateLimiter tokenBucketLuaRateLimiter(StringRedisTemplate redisTemplate) {
      return new TokenBucketLuaRateLimiter(redisTemplate);
    }

    @Bean
    @ConditionalOnBean(TokenBucketLuaRateLimiter.class)
    RateLimitService rateLimitService(
        RateLimitProps props,
        TenantRateLimitRegistry registry,
        RateLimitMetricsRecorder metricsRecorder,
        RateLimitKeyGenerator keyGenerator,
        TokenBucketLuaRateLimiter tokenBucket,
        RateLimitBypassEvaluator bypassEvaluator,
        ObjectProvider<AuditService> auditService) {
      return new RateLimitService(props, registry, metricsRecorder, keyGenerator, tokenBucket, bypassEvaluator,
          auditService.getIfAvailable());
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnBean({RedisConnectionFactory.class, RateLimitService.class})
    @ConditionalOnProperty(prefix = "shared.ratelimit.dynamic", name = "enabled", havingValue = "true",
        matchIfMissing = true)
    RateLimitSubscriptionListener rateLimitSubscriptionListener(
        RedisConnectionFactory connectionFactory,
        RateLimitProps props,
        TenantRateLimitRegistry registry) {
      props.applyDefaults();
      return new RateLimitSubscriptionListener(connectionFactory, props.getDynamic().getSubscriptionChannel(), registry);
    }
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(name = {"jakarta.servlet.Filter", "org.springframework.boot.web.servlet.FilterRegistrationBean"})
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
  @ConditionalOnBean(RateLimitService.class)
  static class ServletFilterConfiguration {

    @Bean
    org.springframework.boot.web.servlet.FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
        RateLimitService rateLimitService) {
      org.springframework.boot.web.servlet.FilterRegistrationBean<RateLimitFilter> registration =
          new org.springframework.boot.web.servlet.FilterRegistrationBean<>();
      registration.setFilter(new RateLimitFilter(rateLimitService));
      registration.addUrlPatterns("/api/v1/*");
      registration.setOrder(1);
      return registration;
    }
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(name = "org.springframework.web.server.WebFilter")
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
  @ConditionalOnBean(RateLimitService.class)
  static class ReactiveFilterConfiguration {

    @Bean
    RateLimitWebFilter rateLimitWebFilter(RateLimitService rateLimitService) {
      return new RateLimitWebFilter(rateLimitService);
    }
  }
}
