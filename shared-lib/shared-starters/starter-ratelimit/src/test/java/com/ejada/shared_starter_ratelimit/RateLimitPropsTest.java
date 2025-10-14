package com.ejada.shared_starter_ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitPropsTest {

  @Test
  void bindsConfiguredValues() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource(
        Map.of(
            "shared.ratelimit.window", "PT30S",
            "shared.ratelimit.default-tier", "pro",
            "shared.ratelimit.tiers.basic.requests-per-minute", "120",
            "shared.ratelimit.tiers.basic.burst-capacity", "300",
            "shared.ratelimit.bypass.super-admin-roles[0]", "ROOT_ADMIN",
            "shared.ratelimit.dynamic.subscription-channel", "custom:updates"
        ));
    RateLimitProps props = new Binder(source)
        .bind("shared.ratelimit", RateLimitProps.class)
        .get();
    props.applyDefaults();

    assertEquals(Duration.ofSeconds(30), props.getWindow());
    assertEquals("PRO", props.getDefaultTier());
    assertEquals(120, props.tier("BASIC").getRequestsPerMinute());
    assertEquals(300, props.tier("BASIC").getBurstCapacity());
    assertEquals("custom:updates", props.getDynamic().getSubscriptionChannel());
    assertEquals("ROOT_ADMIN", props.getBypass().getSuperAdminRoles().get(0));
  }
}
