package com.ejada.gateway.config;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.util.StringUtils;

/**
 * Gateway specific extensions for rate limiting behaviour.
 */
@ConfigurationProperties(prefix = "gateway.ratelimit")
public class GatewayRateLimitProperties {

  private static final double DEFAULT_BURST_MULTIPLIER = 1.5d;

  private Map<String, TierLimit> tierLimits = new HashMap<>();

  private double burstMultiplier = DEFAULT_BURST_MULTIPLIER;

  private boolean allowInternalBypass;

  private String internalSharedSecret;

  public Map<String, TierLimit> getTierLimits() {
    return Collections.unmodifiableMap(tierLimits);
  }

  public void setTierLimits(Map<String, String> limits) {
    this.tierLimits = new HashMap<>();
    if (limits == null) {
      return;
    }
    limits.forEach((tier, spec) -> this.tierLimits.put(normalizeTier(tier), TierLimit.parse(spec)));
  }

  public double getBurstMultiplier() {
    return burstMultiplier > 0 ? burstMultiplier : DEFAULT_BURST_MULTIPLIER;
  }

  public void setBurstMultiplier(double burstMultiplier) {
    this.burstMultiplier = burstMultiplier;
  }

  public boolean isAllowInternalBypass() {
    return allowInternalBypass;
  }

  public void setAllowInternalBypass(boolean allowInternalBypass) {
    this.allowInternalBypass = allowInternalBypass;
  }

  public String getInternalSharedSecret() {
    return internalSharedSecret;
  }

  public void setInternalSharedSecret(String internalSharedSecret) {
    this.internalSharedSecret = internalSharedSecret;
  }

  public TierLimit resolveTier(String tier) {
    if (!StringUtils.hasText(tier)) {
      return null;
    }
    return tierLimits.get(normalizeTier(tier));
  }

  private String normalizeTier(String tier) {
    if (!StringUtils.hasText(tier)) {
      return "default";
    }
    return tier.trim().toLowerCase(Locale.ROOT);
  }

  /**
   * Tier specific configuration (capacity + window).
   */
  public record TierLimit(int capacity, Duration window) {

    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(1);

    static TierLimit parse(String spec) {
      if (!StringUtils.hasText(spec)) {
        return new TierLimit(0, DEFAULT_WINDOW);
      }
      String[] parts = spec.split(":", 2);
      int capacity = 0;
      Duration window = DEFAULT_WINDOW;
      if (parts.length > 0 && StringUtils.hasText(parts[0])) {
        try {
          capacity = Math.max(0, Integer.parseInt(parts[0].trim()));
        } catch (NumberFormatException ignored) {
          capacity = 0;
        }
      }
      if (parts.length == 2 && StringUtils.hasText(parts[1])) {
        try {
          window = DurationStyle.detectAndParse(parts[1].trim(), DurationStyle.SUFFIX);
        } catch (Exception ignored) {
          window = DEFAULT_WINDOW;
        }
      }
      return new TierLimit(capacity, window);
    }

    public TierLimit {
      Objects.requireNonNull(window, "window");
      int normalizedCapacity = Math.max(1, capacity);
      Duration normalizedWindow = window.isZero() || window.isNegative() ? DEFAULT_WINDOW : window;
      capacity = normalizedCapacity;
      window = normalizedWindow;
    }
  }
}

