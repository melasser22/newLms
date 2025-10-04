package com.ejada.gateway.config;

import com.ejada.gateway.filter.RequestBodyTransformationGatewayFilterFactory;
import com.ejada.gateway.cache.CacheRefreshService;
import com.ejada.gateway.filter.ResponseBodyTransformationGatewayFilterFactory;
import com.ejada.gateway.metrics.GatewayMetrics;
import com.ejada.gateway.transformation.HeaderTransformationService;
import com.ejada.gateway.transformation.ResponseBodyTransformer;
import com.ejada.gateway.transformation.ResponseCacheService;
import com.ejada.gateway.transformation.TransformationRuleCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

/**
 * Configuration wiring for gateway transformation and caching components.
 */
@Configuration
@EnableConfigurationProperties({GatewayTransformationProperties.class, GatewayCacheProperties.class, GatewayLimitsProperties.class})
public class GatewayTransformationConfiguration {

  @Bean
  public TransformationRuleCache transformationRuleCache(GatewayTransformationProperties properties) {
    return new TransformationRuleCache(properties);
  }

  @Bean
  public HeaderTransformationService headerTransformationService(GatewayTransformationProperties properties) {
    return new HeaderTransformationService(properties);
  }

  @Bean
  public GatewayMetrics gatewayMetrics(MeterRegistry meterRegistry) {
    return new GatewayMetrics(meterRegistry);
  }

  @Bean
  public RequestBodyTransformationGatewayFilterFactory requestBodyTransformationGatewayFilterFactory(
      TransformationRuleCache ruleCache,
      HeaderTransformationService headerTransformationService,
      GatewayLimitsProperties limitsProperties,
      GatewayMetrics gatewayMetrics) {
    return new RequestBodyTransformationGatewayFilterFactory(ruleCache, headerTransformationService, limitsProperties, gatewayMetrics);
  }

  @Bean
  public ResponseCacheService responseCacheService(GatewayCacheProperties cacheProperties,
      ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider,
      ObjectMapper objectMapper,
      GatewayMetrics metrics) {
    ReactiveStringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
    return new ResponseCacheService(cacheProperties, redisTemplate, objectMapper, metrics);
  }

  @Bean
  public ResponseBodyTransformer responseBodyTransformer(GatewayTransformationProperties properties,
      ObjectMapper objectMapper,
      GatewayMetrics metrics,
      ObjectProvider<BuildProperties> buildPropertiesProvider) {
    BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
    return new ResponseBodyTransformer(properties, objectMapper, metrics, buildProperties);
  }

  @Bean
  public ResponseBodyTransformationGatewayFilterFactory responseBodyTransformationGatewayFilterFactory(
      HeaderTransformationService headerTransformationService,
      ResponseBodyTransformer responseBodyTransformer,
      ObjectProvider<ResponseCacheService> cacheServiceProvider,
      ObjectProvider<CacheRefreshService> cacheRefreshServiceProvider) {
    return new ResponseBodyTransformationGatewayFilterFactory(headerTransformationService,
        responseBodyTransformer,
        cacheServiceProvider.getIfAvailable(),
        cacheRefreshServiceProvider.getIfAvailable());
  }
}

