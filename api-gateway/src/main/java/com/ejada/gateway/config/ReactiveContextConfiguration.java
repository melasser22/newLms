package com.ejada.gateway.config;

import com.ejada.gateway.context.ReactiveRequestContextFilter;
import com.ejada.gateway.observability.GatewayTracingHelper;
import com.ejada.gateway.ratelimit.ReactiveRateLimiterFilter;
import com.ejada.shared_starter_ratelimit.RateLimitProps;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.web.server.WebFilter;

/**
 * Reactive equivalents for the servlet-based context/rate-limit filters.
 */
@Configuration
@EnableConfigurationProperties({CoreAutoConfiguration.CoreProps.class, RateLimitProps.class,
    GatewayRateLimitProperties.class})
public class ReactiveContextConfiguration {

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  @ConditionalOnMissingBean(ReactiveRequestContextFilter.class)
  @ConditionalOnProperty(prefix = "shared.core.context", name = "enabled", havingValue = "true", matchIfMissing = true)
  public WebFilter reactiveRequestContextFilter(CoreAutoConfiguration.CoreProps props,
      @Qualifier("jacksonObjectMapper") @Nullable ObjectMapper jacksonObjectMapper,
      ObjectProvider<ObjectMapper> objectMapperProvider) {
    ObjectMapper mapper = (jacksonObjectMapper != null)
        ? jacksonObjectMapper
        : objectMapperProvider.getIfAvailable(ObjectMapper::new);
    return new ReactiveRequestContextFilter(props, mapper);
  }

  @Bean
  @ConditionalOnClass(ReactiveStringRedisTemplate.class)
  @ConditionalOnBean(ReactiveStringRedisTemplate.class)
  @ConditionalOnProperty(prefix = "shared.ratelimit", name = "enabled", havingValue = "true")
  public ReactiveRateLimiterFilter reactiveRateLimiterFilter(
      ReactiveStringRedisTemplate redisTemplate,
      RateLimitProps props,
      KeyResolver keyResolver,
      GatewayRateLimitProperties gatewayRateLimitProperties,
      @Qualifier("jacksonObjectMapper") @Nullable ObjectMapper jacksonObjectMapper,
      ObjectProvider<ObjectMapper> objectMapperProvider,
      ObjectProvider<MeterRegistry> meterRegistryProvider,
      ObjectProvider<GatewayTracingHelper> tracingHelperProvider) {
    ObjectMapper mapper = (jacksonObjectMapper != null)
        ? jacksonObjectMapper
        : objectMapperProvider.getIfAvailable();
    MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
    return new ReactiveRateLimiterFilter(redisTemplate, props, keyResolver, mapper,
        gatewayRateLimitProperties, meterRegistry, tracingHelperProvider.getIfAvailable());
  }
}
