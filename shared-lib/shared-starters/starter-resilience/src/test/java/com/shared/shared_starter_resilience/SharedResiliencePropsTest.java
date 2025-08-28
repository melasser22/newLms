package com.shared.shared_starter_resilience;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SharedResiliencePropsTest {

  @Test
  void bindsConfiguredValues() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource(
        Map.of("shared.resilience.http-timeout-ms", "1000",
               "shared.resilience.connect-timeout-ms", "500"));
    SharedResilienceProps props = Binder.get(source)
        .bind("shared.resilience", SharedResilienceProps.class).get();
    assertEquals(1000, props.getHttpTimeoutMs());
    assertEquals(500, props.getConnectTimeoutMs());
  }
}
