package com.ejada.gateway.config;

import com.ejada.gateway.filter.GatewayAccessLogFilter;
import com.ejada.gateway.filter.GatewayMetricsFilter;
import com.ejada.gateway.observability.GatewayTracingHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PostConstruct;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires gateway specific metrics, logging, and tracing helpers.
 */
@Configuration
public class GatewayMetricsConfiguration {

  private final MeterRegistry meterRegistry;
  private final GatewayLoggingProperties loggingProperties;
  private final GatewayTracingProperties tracingProperties;
  private final ObjectProvider<CircuitBreakerRegistry> circuitBreakerRegistryProvider;
  private final ObjectProvider<Tracer> tracerProvider;
  private final Set<String> registeredCircuitBreakerMeters = ConcurrentHashMap.newKeySet();

  public GatewayMetricsConfiguration(MeterRegistry meterRegistry,
      GatewayLoggingProperties loggingProperties,
      GatewayTracingProperties tracingProperties,
      ObjectProvider<CircuitBreakerRegistry> circuitBreakerRegistryProvider,
      ObjectProvider<Tracer> tracerProvider) {
    this.meterRegistry = meterRegistry;
    this.loggingProperties = loggingProperties;
    this.tracingProperties = tracingProperties;
    this.circuitBreakerRegistryProvider = circuitBreakerRegistryProvider;
    this.tracerProvider = tracerProvider;
  }

  @Bean
  public GatewayTracingHelper gatewayTracingHelper() {
    return new GatewayTracingHelper(tracerProvider.getIfAvailable(), tracingProperties);
  }

  @Bean
  public GatewayMetricsFilter gatewayMetricsFilter(GatewayTracingHelper tracingHelper) {
    return new GatewayMetricsFilter(meterRegistry, tracingHelper);
  }

  @Bean
  public GatewayAccessLogFilter gatewayAccessLogFilter(ObjectMapper objectMapper,
      GatewayTracingHelper tracingHelper) {
    return new GatewayAccessLogFilter(loggingProperties, objectMapper, tracingHelper);
  }

  @PostConstruct
  void bindCircuitBreakerMetrics() {
    CircuitBreakerRegistry registry = circuitBreakerRegistryProvider.getIfAvailable();
    if (registry == null) {
      return;
    }
    registry.getAllCircuitBreakers().forEach(this::registerCircuitBreakerMeters);
    registry.getEventPublisher().onEntryAdded(event -> registerCircuitBreakerMeters(event.getAddedEntry()));
  }

  private void registerCircuitBreakerMeters(CircuitBreaker circuitBreaker) {
    EnumSet<CircuitBreaker.State> states = EnumSet.allOf(CircuitBreaker.State.class);
    for (CircuitBreaker.State state : states) {
      String key = circuitBreaker.getName() + ":" + state.name();
      if (!registeredCircuitBreakerMeters.add(key)) {
        continue;
      }
      Gauge.builder("gateway.circuit_breaker.state", circuitBreaker,
              cb -> cb.getState() == state ? 1.0 : 0.0)
          .description("State of resilience4j circuit breakers exposed by the gateway")
          .tags("serviceName", circuitBreaker.getName(),
              "state", state.name().toLowerCase(Locale.ROOT))
          .register(meterRegistry);
    }
  }
}
