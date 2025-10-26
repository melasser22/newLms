package com.ejada.gateway.config;

import com.ejada.gateway.filter.GatewayAccessLogFilter;
import com.ejada.gateway.filter.GatewayMetricsFilter;
import com.ejada.gateway.metrics.TenantRequestMetricsTracker;
import com.ejada.gateway.observability.GatewayTracingHelper;
import com.ejada.gateway.resilience.CircuitBreakerRecoveryTester;
import com.ejada.gateway.routes.service.RouteCallAuditService;
import com.ejada.gateway.routes.service.RouteVariantService;
import com.ejada.gateway.resilience.TenantCircuitBreakerMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PostConstruct;
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
  private final TenantCircuitBreakerMetrics circuitBreakerMetrics;
  private final CircuitBreakerRecoveryTester recoveryTester;

  public GatewayMetricsConfiguration(MeterRegistry meterRegistry,
      GatewayLoggingProperties loggingProperties,
      GatewayTracingProperties tracingProperties,
      ObjectProvider<CircuitBreakerRegistry> circuitBreakerRegistryProvider,
      ObjectProvider<Tracer> tracerProvider,
      TenantCircuitBreakerMetrics circuitBreakerMetrics,
      CircuitBreakerRecoveryTester recoveryTester) {
    this.meterRegistry = meterRegistry;
    this.loggingProperties = loggingProperties;
    this.tracingProperties = tracingProperties;
    this.circuitBreakerRegistryProvider = circuitBreakerRegistryProvider;
    this.tracerProvider = tracerProvider;
    this.circuitBreakerMetrics = circuitBreakerMetrics;
    this.recoveryTester = recoveryTester;
  }

  @Bean
  public GatewayTracingHelper gatewayTracingHelper() {
    return new GatewayTracingHelper(tracerProvider.getIfAvailable(), tracingProperties);
  }

  @Bean
  public GatewayMetricsFilter gatewayMetricsFilter(GatewayTracingHelper tracingHelper,
      RouteVariantService variantService,
      TenantRequestMetricsTracker tenantMetricsTracker) {
    return new GatewayMetricsFilter(meterRegistry, tracingHelper, variantService, tenantMetricsTracker);
  }

  @Bean
  public GatewayAccessLogFilter gatewayAccessLogFilter(ObjectMapper objectMapper,
      GatewayTracingHelper tracingHelper,
      RouteCallAuditService routeCallAuditService) {
    return new GatewayAccessLogFilter(loggingProperties, objectMapper, tracingHelper, routeCallAuditService);
  }

  @PostConstruct
  void bindCircuitBreakerMetrics() {
    CircuitBreakerRegistry registry = circuitBreakerRegistryProvider.getIfAvailable();
    if (registry == null) {
      return;
    }
    registry.getAllCircuitBreakers().forEach(this::monitorCircuitBreaker);
    registry.getEventPublisher().onEntryAdded(event -> monitorCircuitBreaker(event.getAddedEntry()));
  }

  private void monitorCircuitBreaker(io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker) {
    circuitBreakerMetrics.bind(circuitBreaker);
    recoveryTester.monitor(circuitBreaker);
  }
}
