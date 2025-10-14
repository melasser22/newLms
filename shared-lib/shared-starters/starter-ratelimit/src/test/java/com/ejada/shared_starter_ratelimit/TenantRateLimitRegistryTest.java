package com.ejada.shared_starter_ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TenantRateLimitRegistryTest {

  @Test
  void resolvesDefaultAndOverrideTiers() {
    RateLimitProps props = new RateLimitProps();
    props.applyDefaults();
    TenantRateLimitRegistry registry = new TenantRateLimitRegistry(props);

    RateLimitTier defaultTier = registry.resolveTier("tenant-1");
    assertEquals("BASIC", defaultTier.name());
    assertEquals(100, defaultTier.requestsPerMinute());

    RateLimitSubscriptionUpdate update = new RateLimitSubscriptionUpdate("tenant-1", "ENTERPRISE", 2500, 3500);
    registry.apply(update);

    RateLimitTier updatedTier = registry.resolveTier("tenant-1");
    assertEquals("ENTERPRISE", updatedTier.name());
    assertEquals(2500, updatedTier.requestsPerMinute());
    assertEquals(3500, updatedTier.burstCapacity());
    assertEquals(Duration.ofMinutes(1), updatedTier.window());
  }
}
