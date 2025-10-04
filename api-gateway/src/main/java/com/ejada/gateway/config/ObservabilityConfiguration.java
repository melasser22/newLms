package com.ejada.gateway.config;

import com.ejada.common.context.ContextManager;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.filter.GatewayAccessLogFilter;
import com.ejada.gateway.config.GatewayLoggingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationRegistry;
import java.util.Optional;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.observation.ServerRequestObservationContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;

/**
 * Ensures that tenant and correlation metadata are attached to observations and MDC when the
 * gateway emits traces and metrics. This complements the shared starters by enforcing the
 * presence of tenantId/traceId on every span emitted from the edge service.
 */
@Configuration
public class ObservabilityConfiguration {

  @Bean
  ObservationFilter tenantObservationFilter(GatewayTracingProperties tracingProperties) {
    return context -> {
      String tenant = ContextManager.Tenant.get();
      if (StringUtils.hasText(tenant)) {
        context.addHighCardinalityKeyValue(KeyValue.of("tenant.id", tenant));
      }
      String correlationId = ContextManager.getCorrelationId();
      if (StringUtils.hasText(correlationId)) {
        context.addHighCardinalityKeyValue(KeyValue.of("correlation.id", correlationId));
      }

      if (context instanceof ServerRequestObservationContext serverContext
          && tracingProperties.getEnhancedTags().isEnabled()) {
        enhanceWithGatewayTags(serverContext);
      }
      return context;
    };
  }

  @Bean
  ObservationRegistryCustomizer<ObservationRegistry> observationRegistryCustomizer(
      ObservationFilter tenantObservationFilter) {
    return registry -> registry.observationConfig()
        .observationFilter(tenantObservationFilter);
  }

  @Bean
  @ConditionalOnProperty(prefix = "gateway.logging.access-log", name = "enabled", havingValue = "true")
  WebFilter gatewayAccessLogFilter(ObjectMapper objectMapper, GatewayLoggingProperties loggingProperties) {
    return new GatewayAccessLogFilter(objectMapper, loggingProperties.getAccessLog());
  }

  private void enhanceWithGatewayTags(ServerRequestObservationContext serverContext) {
    Object carrier = serverContext.getCarrier();
    if (!(carrier instanceof ServerWebExchange exchange)) {
      return;
    }
    Optional.ofNullable(exchange.getAttribute(GatewayRequestAttributes.TENANT_ID))
        .filter(StringUtils::hasText)
        .ifPresent(tenant -> serverContext.addHighCardinalityKeyValue(KeyValue.of("tenant_id", tenant)));
    Optional.ofNullable(exchange.getAttribute(GatewayRequestAttributes.SUBSCRIPTION_TIER))
        .filter(StringUtils::hasText)
        .ifPresent(tier -> serverContext.addHighCardinalityKeyValue(KeyValue.of("subscription_tier", tier)));
    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    if (route != null && StringUtils.hasText(route.getId())) {
      serverContext.addHighCardinalityKeyValue(KeyValue.of("route_id", route.getId()));
    }
  }
}
