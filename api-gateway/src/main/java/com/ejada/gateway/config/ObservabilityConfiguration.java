package com.ejada.gateway.config;

import com.ejada.common.context.ContextManager;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Ensures that tenant and correlation metadata are attached to observations and MDC when the
 * gateway emits traces and metrics. This complements the shared starters by enforcing the
 * presence of tenantId/traceId on every span emitted from the edge service.
 */
@Configuration
public class ObservabilityConfiguration {

  @Bean
  ObservationFilter tenantObservationFilter() {
    return context -> {
      String tenant = ContextManager.Tenant.get();
      if (StringUtils.hasText(tenant)) {
        context.addHighCardinalityKeyValue(KeyValue.of("tenant.id", tenant));
      }
      String correlationId = ContextManager.getCorrelationId();
      if (StringUtils.hasText(correlationId)) {
        context.addHighCardinalityKeyValue(KeyValue.of("correlation.id", correlationId));
      }
      return context;
    };
  }

  @Bean
  ObservationRegistryCustomizer<ObservationRegistry> observationRegistryCustomizer(
      ObservationFilter tenantObservationFilter) {
    return registry -> registry.observationConfig()
        .observationFilter(tenantObservationFilter)
        .registerThreadLocalAccessor(new ObservationThreadLocalAccessor());
  }
}
