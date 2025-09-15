package com.ejada.shared_starter_ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitPropsTest {

  @Test
  void bindsConfiguredValues() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource(
        Map.of("shared.ratelimit.capacity", "10",
               "shared.ratelimit.refill-per-minute", "5",
               "shared.ratelimit.key-strategy", "ip"));
    RateLimitProps props = new Binder(source)
        .bind("shared.ratelimit", RateLimitProps.class)
        .get();
    assertEquals(10, props.getCapacity());
    assertEquals(5, props.getRefillPerMinute());
    assertEquals("ip", props.getKeyStrategy());
  }
}
