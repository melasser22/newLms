package com.ejada.actuator.starter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.actuator.starter.config.SharedActuatorProperties;
import com.ejada.actuator.starter.health.SlaHealthIndicator;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class SlaReportControllerTest {

  @Test
  void reportUsesDirectIndicatorWhenRegistryUnavailable() {
    SharedActuatorProperties properties = new SharedActuatorProperties();
    properties.getSlaReport().setSlaCompliant(false);
    SlaHealthIndicator indicator = new SlaHealthIndicator(properties);

    SlaReportController controller =
        new SlaReportController(
            new EmptyObjectProvider<>(), new EmptyObjectProvider<>(), new StaticObjectProvider<>(indicator));

    Map<String, Object> report = controller.report();

    assertThat(report.get("status")).isEqualTo("UP");
    Map<?, ?> components = Map.class.cast(report.get("components"));
    Map<?, ?> indicatorBody = Map.class.cast(components.get("slaHealthIndicator"));
    assertThat(indicatorBody.get("status")).isEqualTo("UP");
    Map<?, ?> details = Map.class.cast(indicatorBody.get("details"));
    assertThat(details.get("sla_compliant")).isEqualTo(false);
    assertThat(details).containsKeys("availability_percent", "last_check");
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
