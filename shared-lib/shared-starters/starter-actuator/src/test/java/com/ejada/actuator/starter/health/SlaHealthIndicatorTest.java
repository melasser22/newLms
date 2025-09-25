package com.ejada.actuator.starter.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.actuator.starter.config.SharedActuatorProperties;
import com.ejada.actuator.starter.metrics.SlaMetricsCalculator;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class SlaHealthIndicatorTest {

  @Test
  void reportsDownWhenSlaIsNotMet() {
    SharedActuatorProperties props = new SharedActuatorProperties();
    props.getSlaMetrics().setSlaTarget(95.0D);

    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    Timer success = Timer.builder("http.server.requests").tag("status", "200").register(registry);
    Timer failure = Timer.builder("http.server.requests").tag("status", "500").register(registry);

    success.record(Duration.ofMillis(5));
    failure.record(Duration.ofMillis(5));
    failure.record(Duration.ofMillis(5));

    SlaMetricsCalculator calculator = new SlaMetricsCalculator(registry, props);
    SlaHealthIndicator indicator = new SlaHealthIndicator(calculator);

    Health health = indicator.getHealth(true);

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("sla_compliant", false);
  }

  @Test
  void reportsUpWhenSlaIsMet() {
    SharedActuatorProperties props = new SharedActuatorProperties();
    props.getSlaMetrics().setSlaTarget(80.0D);
    props.getSlaMetrics().setSloTarget(60.0D);

    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    Timer success = Timer.builder("http.server.requests").tag("status", "200").register(registry);
    Timer failure = Timer.builder("http.server.requests").tag("status", "500").register(registry);

    success.record(Duration.ofMillis(5));
    success.record(Duration.ofMillis(5));
    failure.record(Duration.ofMillis(5));

    SlaMetricsCalculator calculator = new SlaMetricsCalculator(registry, props);
    SlaHealthIndicator indicator = new SlaHealthIndicator(calculator);

    Health health = indicator.getHealth(true);

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("sla_compliant", true);
    assertThat(health.getDetails()).containsEntry("sli", 66.667D);
    assertThat(health.getDetails()).containsEntry("sla", 80.0D);
  }
}

