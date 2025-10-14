package com.ejada.shared_starter_ratelimit;

import com.ejada.shared_starter_ratelimit.RateLimitProps.TierProperties;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Keeps track of tenant-specific overrides and resolves the effective tier.
 */
public class TenantRateLimitRegistry {

  private final RateLimitProps props;
  private final ConcurrentMap<String, TenantOverride> overrides = new ConcurrentHashMap<>();

  public TenantRateLimitRegistry(RateLimitProps props) {
    this.props = props;
    props.applyDefaults();
  }

  public RateLimitTier resolveTier(String tenantId) {
    String safeTenant = tenantId == null ? "" : tenantId;
    TenantOverride override = overrides.get(safeTenant);
    TierProperties base = props.tier(Optional.ofNullable(override).map(TenantOverride::tier).orElse(null));
    int requestsPerMinute = Optional.ofNullable(override)
        .map(TenantOverride::requestsPerMinute)
        .orElse(base.getRequestsPerMinute());
    int burst = Optional.ofNullable(override)
        .map(TenantOverride::burstCapacity)
        .orElse(base.getBurstCapacity());
    if (burst < requestsPerMinute) {
      burst = requestsPerMinute;
    }
    Duration window = props.getWindow();
    return new RateLimitTier(determineTierName(override, base), requestsPerMinute, burst, window);
  }

  private String determineTierName(TenantOverride override, TierProperties base) {
    if (override != null && override.tier() != null && !override.tier().isBlank()) {
      return override.tier();
    }
    return props.getDefaultTier() != null ? props.getDefaultTier() : "BASIC";
  }

  public void apply(RateLimitSubscriptionUpdate update) {
    Objects.requireNonNull(update, "update");
    String tenantId = Optional.ofNullable(update.tenantId()).orElse("");
    overrides.compute(tenantId, (key, existing) ->
        TenantOverride.merge(existing, update));
  }

  public record TenantOverride(String tenantId, String tier, Integer requestsPerMinute, Integer burstCapacity) {
    static TenantOverride merge(TenantOverride current, RateLimitSubscriptionUpdate update) {
      String tier = normalizeTier(update.tier());
      Integer rpm = update.requestsPerMinute();
      Integer burst = update.burstCapacity();
      if (current == null) {
        return new TenantOverride(update.tenantId(), tier, rpm, burst);
      }
      String resolvedTier = tier != null ? tier : current.tier;
      Integer resolvedRpm = rpm != null ? rpm : current.requestsPerMinute;
      Integer resolvedBurst = burst != null ? burst : current.burstCapacity;
      if (resolvedTier == null && resolvedRpm == null && resolvedBurst == null) {
        return null;
      }
      return new TenantOverride(update.tenantId(), resolvedTier, resolvedRpm, resolvedBurst);
    }

    private static String normalizeTier(String tier) {
      if (tier == null) {
        return null;
      }
      return tier.trim().toUpperCase(Locale.ROOT);
    }
  }
}
