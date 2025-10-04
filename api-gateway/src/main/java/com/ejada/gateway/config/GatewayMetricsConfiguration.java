package com.ejada.gateway.config;

import com.ejada.gateway.metrics.GatewayCircuitBreakerStateMetrics;
import com.ejada.gateway.metrics.GatewayMetrics;
import com.ejada.gateway.metrics.GatewayMetricsWebFilter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

/**
 * Central configuration for custom gateway metrics and binders.
 */
@Configuration
public class GatewayMetricsConfiguration {

  @Bean
  public GatewayMetrics gatewayMetrics(MeterRegistry meterRegistry) {
    return new GatewayMetrics(meterRegistry);
  }

  @Bean
  public WebFilter gatewayMetricsWebFilter(GatewayMetrics gatewayMetrics) {
    return new GatewayMetricsWebFilter(gatewayMetrics);
  }

  @Bean
  @ConditionalOnBean(CircuitBreakerRegistry.class)
  public MeterBinder gatewayCircuitBreakerStateMetrics(CircuitBreakerRegistry registry) {
    return new GatewayCircuitBreakerStateMetrics(registry);
  }
}
