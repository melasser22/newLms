package com.ejada.actuator.starter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.actuator.starter.config.SharedActuatorProperties;
import com.ejada.actuator.starter.health.SlaHealthIndicator;
import com.ejada.actuator.starter.metrics.SlaMetricsCalculator;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class SlaReportControllerTest {

  @Test
  void reportUsesDirectIndicatorWhenRegistryUnavailable() {
    SharedActuatorProperties properties = new SharedActuatorProperties();
    properties.getSlaMetrics().setSlaTarget(90.0D);
    properties.getSlaMetrics().setSloTarget(95.0D);

    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    Timer.builder("http.server.requests").tag("status", "200").register(registry).record(Duration.ofMillis(5));
    Timer failureTimer =
        Timer.builder("http.server.requests").tag("status", "500").register(registry);
    failureTimer.record(Duration.ofMillis(5));
    failureTimer.record(Duration.ofMillis(5));

    SlaMetricsCalculator calculator = new SlaMetricsCalculator(registry, properties);
    SlaHealthIndicator indicator = new SlaHealthIndicator(calculator);

    SlaReportController controller =
        new SlaReportController(
            new EmptyObjectProvider<>(), new EmptyObjectProvider<>(), new StaticObjectProvider<>(indicator));

    Map<String, Object> report = controller.report();

    assertThat(report.get("status")).isEqualTo("UP");
    Map<String, ?> components = castToMap(report.get("components"));
    Map<String, ?> indicatorBody = castToMap(components.get("slaHealthIndicator"));
    assertThat(indicatorBody.get("status")).isEqualTo("UP");
    Map<String, ?> details = castToMap(indicatorBody.get("details"));
    assertThat(details.get("sla_compliant")).isEqualTo(false);
    assertThat(details)
        .containsKeys(
            "availability_percent",
            "last_check",
            "sli",
            "slo",
            "slo_met",
            "sla",
            "total_requests",
            "failed_requests",
            "error_budget_remaining");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, ?> castToMap(Object value) {
    return (Map<String, ?>) value;
  }

  private static final class EmptyObjectProvider<T> implements ObjectProvider<T> {

    @Override
    public T getObject(Object... args) {
      throw new IllegalStateException("No instance available");
    }

    @Override
    public T getIfAvailable() {
      return null;
    }

    @Override
    public T getIfUnique() {
      return null;
    }

    @Override
    public Stream<T> stream() {
      return Stream.empty();
    }

    @Override
    public Stream<T> orderedStream() {
      return Stream.empty();
    }

    @Override
    public T getObject() {
      throw new IllegalStateException("No instance available");
    }
  }

  private static final class StaticObjectProvider<T> implements ObjectProvider<T> {

    private final T instance;

    private StaticObjectProvider(T instance) {
      this.instance = instance;
    }

    @Override
    public T getObject(Object... args) {
      return instance;
    }

    @Override
    public T getIfAvailable() {
      return instance;
    }

    @Override
    public T getIfUnique() {
      return instance;
    }

    @Override
    public Stream<T> stream() {
      return Stream.of(instance);
    }

    @Override
    public Stream<T> orderedStream() {
      return Stream.of(instance);
    }

    @Override
    public T getObject() {
      return instance;
    }
  }
}
