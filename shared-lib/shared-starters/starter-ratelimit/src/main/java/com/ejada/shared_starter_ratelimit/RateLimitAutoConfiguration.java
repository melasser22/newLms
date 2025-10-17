package com.ejada.shared_starter_ratelimit;

import com.ejada.audit.starter.api.AuditService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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

  @Bean
  @ConditionalOnBean(RateLimitService.class)
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
  public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(RateLimitService rateLimitService) {
    FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new RateLimitFilter(rateLimitService));
    registration.addUrlPatterns("/api/v1/*");
    registration.setOrder(1);
    return registration;
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
    RateLimitSubscriptionListener rateLimitSubscriptionListener(
        RedisConnectionFactory connectionFactory,
        RateLimitProps props,
        TenantRateLimitRegistry registry) {
      props.applyDefaults();
      return new RateLimitSubscriptionListener(connectionFactory, props.getDynamic().getSubscriptionChannel(), registry);
    }
  }
}
