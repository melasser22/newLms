package com.ejada.actuator.starter.endpoints;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.actuator.starter.config.SharedActuatorProperties;
import com.ejada.actuator.starter.metrics.SlaMetricsCalculator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class SlaMetricsEndpointTest {

  @Test
  void calculatesRatiosAndBudgetFromHttpServerRequests() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    Timer success = Timer.builder("http.server.requests")
        .tag("outcome", "SUCCESS")
        .tag("status", "200")
        .register(registry);
    Timer failure = Timer.builder("http.server.requests")
        .tag("outcome", "SERVER_ERROR")
        .tag("status", "500")
        .register(registry);

    for (int i = 0; i < 95; i++) {
      success.record(Duration.ofMillis(10));
    }
    for (int i = 0; i < 5; i++) {
      failure.record(Duration.ofMillis(10));
    }

    SharedActuatorProperties props = new SharedActuatorProperties();
    props.getSlaMetrics().setSloTarget(94.0D);
    props.getSlaMetrics().setSlaTarget(90.0D);

    SlaMetricsCalculator calculator = new SlaMetricsCalculator(registry, props);
    SlaMetricsEndpoint endpoint = new SlaMetricsEndpoint(calculator);

    var result = endpoint.slaMetrics();

    assertThat(result).containsEntry("meter", "http.server.requests");
    assertThat(result).containsEntry("totalRequests", 100L);
    assertThat(result).containsEntry("successfulRequests", 95L);
    assertThat(result).containsEntry("failedRequests", 5L);
    assertThat(result).containsEntry("sli", 95.0D);
    assertThat(result).containsEntry("sloTarget", 94.0D);
    assertThat(result).containsEntry("slaTarget", 90.0D);
    assertThat(result).containsEntry("sloMet", true);
    assertThat(result).containsEntry("slaMet", true);
    assertThat(result).containsEntry("errorBudget", 6.0D);
    assertThat(result).containsEntry("errorBudgetConsumed", 5.0D);
    assertThat(result).containsEntry("errorBudgetRemaining", 1.0D);
  }

  @Test
  void treatsEmptyRegistryAsFullyCompliant() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    SharedActuatorProperties props = new SharedActuatorProperties();

    SlaMetricsCalculator calculator = new SlaMetricsCalculator(registry, props);
    SlaMetricsEndpoint endpoint = new SlaMetricsEndpoint(calculator);

    var result = endpoint.slaMetrics();

    assertThat(result).containsEntry("totalRequests", 0L);
    assertThat(result).containsEntry("successfulRequests", 0L);
    assertThat(result).containsEntry("failedRequests", 0L);
    assertThat(result).containsEntry("sli", 100.0D);
    assertThat(result).containsEntry("sloMet", true);
    assertThat(result).containsEntry("slaMet", true);
    assertThat(result).containsEntry("errorBudgetConsumed", 0.0D);
    assertThat(result).containsEntry("errorBudgetRemaining", 0.1D);
  }
}
