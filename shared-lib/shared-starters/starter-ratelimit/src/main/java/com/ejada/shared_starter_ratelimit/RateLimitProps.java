package com.ejada.shared_starter_ratelimit;

import com.ejada.common.BaseStarterProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Rate limiting properties.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "shared.ratelimit")
public class RateLimitProps implements BaseStarterProperties {

  /** Duration of the logical evaluation window (typically one minute). */
  private Duration window = Duration.ofMinutes(1);

  /** Default tier to fall back to when a tenant has no explicit subscription. */
  private String defaultTier = "BASIC";

  /** Tier catalogue keyed by tier name. */
  private Map<String, TierProperties> tiers = new LinkedHashMap<>();

  /** Multi-dimensional strategy configuration. */
  private MultiDimensionalProperties multidimensional = new MultiDimensionalProperties();

  /** Bypass configuration for privileged identities. */
  private BypassProperties bypass = new BypassProperties();

  /** Dynamic runtime configuration, including Redis pub/sub channel. */
  private DynamicProperties dynamic = new DynamicProperties();

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private Integer legacyCapacity;

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private Integer legacyBurstCapacity;

  /** Apply defaults and normalisation after binding. */
  public void applyDefaults() {
    if (window == null || window.isZero() || window.isNegative()) {
      window = Duration.ofMinutes(1);
    }

    applyLegacyCapacityOverrides();

    tiers = normaliseTiers(tiers);

    String normalisedDefault = normalizeTierName(defaultTier);
    if (!tiers.containsKey(normalisedDefault)) {
      TierProperties builtIn = builtInTier(normalisedDefault);
      if (builtIn != null) {
        tiers.put(normalisedDefault, builtIn);
      }
    }

    if (!tiers.containsKey(normalisedDefault)) {
      defaultTier = "BASIC";
    } else {
      defaultTier = normalisedDefault;
    }

    multidimensional.applyDefaults();
    bypass.applyDefaults();
    dynamic.applyDefaults();
  }

  public void setCapacity(Integer capacity) {
    this.legacyCapacity = capacity;
  }

  public void setBurstCapacity(Integer burstCapacity) {
    this.legacyBurstCapacity = burstCapacity;
  }

  private void applyLegacyCapacityOverrides() {
    boolean hasCapacity = legacyCapacity != null && legacyCapacity > 0;
    boolean hasBurst = legacyBurstCapacity != null && legacyBurstCapacity > 0;

    if (!hasCapacity && !hasBurst) {
      return;
    }

    if (tiers == null) {
      tiers = new LinkedHashMap<>();
    }

    String targetTier = normalizeTierName(defaultTier);
    if (!StringUtils.hasText(targetTier)) {
      targetTier = "BASIC";
    }

    TierProperties tier = tiers.computeIfAbsent(targetTier, key -> new TierProperties());

    if (hasCapacity) {
      tier.setRequestsPerMinute(Math.max(1, legacyCapacity));
    }

    if (hasBurst) {
      int burst = Math.max(legacyBurstCapacity, tier.getRequestsPerMinute());
      tier.setBurstCapacity(burst);
    }
  }

  public TierProperties tier(String name) {
    if (tiers == null || tiers.isEmpty()) {
      tiers = normaliseTiers(Map.of());
    }
    String key = normalizeTierName(name);
    TierProperties properties = tiers.get(key);
    if (properties == null) {
      properties = tiers.get(defaultTier);
    }
    return properties == null ? TierProperties.basic() : properties;
  }

  private Map<String, TierProperties> normaliseTiers(Map<String, TierProperties> input) {
    Map<String, TierProperties> result = new LinkedHashMap<>();
    if (input != null) {
      input.forEach((key, value) -> {
        if (value != null) {
          result.put(normalizeTierName(key), value.normalisedCopy());
        }
      });
    }

    if (result.isEmpty()) {
      result.put("BASIC", TierProperties.basic());
      result.put("PRO", TierProperties.pro());
      result.put("ENTERPRISE", TierProperties.enterprise());
    }

    return result;
  }

  private TierProperties builtInTier(String tierName) {
    return switch (tierName) {
      case "BASIC" -> TierProperties.basic();
      case "PRO" -> TierProperties.pro();
      case "ENTERPRISE" -> TierProperties.enterprise();
      default -> null;
    };
  }

  private static String normalizeTierName(String name) {
    if (name == null) {
      return "";
    }
    return name.trim().toUpperCase(Locale.ROOT);
  }

  @Getter
  @Setter
  public static class TierProperties {
    private int requestsPerMinute;
    private int burstCapacity;

    public static TierProperties basic() {
      TierProperties props = new TierProperties();
      props.setRequestsPerMinute(100);
      props.setBurstCapacity(150);
      return props;
    }

    public static TierProperties pro() {
      TierProperties props = new TierProperties();
      props.setRequestsPerMinute(500);
      props.setBurstCapacity(750);
      return props;
    }

    public static TierProperties enterprise() {
      TierProperties props = new TierProperties();
      props.setRequestsPerMinute(2000);
      props.setBurstCapacity(2600);
      return props;
    }

    private TierProperties normalisedCopy() {
      TierProperties copy = new TierProperties();
      int limit = requestsPerMinute <= 0 ? 1 : requestsPerMinute;
      copy.setRequestsPerMinute(limit);
      int burst = burstCapacity <= 0 ? Math.max(limit, (int) Math.round(limit * 1.3)) : burstCapacity;
      if (burst < limit) {
        burst = limit;
      }
      copy.setBurstCapacity(burst);
      return copy;
    }
  }

  @Getter
  @Setter
  public static class MultiDimensionalProperties {
    private List<StrategyProperties> strategies = new ArrayList<>();

    void applyDefaults() {
      if (strategies == null) {
        strategies = new ArrayList<>();
      } else if (!(strategies instanceof ArrayList<?>)) {
        strategies = new ArrayList<>(strategies);
      }
      if (strategies.isEmpty()) {
        strategies.add(StrategyProperties.of("tenant", List.of(Dimension.TENANT)));
        strategies.add(StrategyProperties.of("tenant_user", List.of(Dimension.TENANT, Dimension.USER)));
        strategies.add(StrategyProperties.of("tenant_ip", List.of(Dimension.TENANT, Dimension.IP)));
        strategies.add(StrategyProperties.of("tenant_endpoint", List.of(Dimension.TENANT, Dimension.ENDPOINT)));
      } else {
        List<StrategyProperties> normalised = strategies.stream()
            .map(strategy -> strategy == null
                ? StrategyProperties.of("tenant", List.of(Dimension.TENANT))
                : strategy.normalised())
            .collect(Collectors.toCollection(ArrayList::new));
        strategies = normalised;
      }
    }
  }

  @Getter
  @Setter
  public static class StrategyProperties {
    private String name;
    private boolean enabled = true;
    private List<Dimension> dimensions = new ArrayList<>();

    public static StrategyProperties of(String name, List<Dimension> dimensions) {
      StrategyProperties props = new StrategyProperties();
      props.setName(name);
      props.setDimensions(new ArrayList<>(dimensions));
      props.setEnabled(true);
      return props;
    }

    StrategyProperties normalised() {
      StrategyProperties copy = new StrategyProperties();
      copy.setEnabled(enabled);
      copy.setName(name == null || name.isBlank() ? UUID.randomUUID().toString() : name.trim().toLowerCase(Locale.ROOT));
      if (dimensions == null || dimensions.isEmpty()) {
        copy.setDimensions(new ArrayList<>(List.of(Dimension.TENANT)));
      } else {
        copy.setDimensions(dimensions.stream().filter(Objects::nonNull).distinct().collect(Collectors.toCollection(ArrayList::new)));
      }
      return copy;
    }
  }

  public enum Dimension {
    TENANT,
    USER,
    IP,
    ENDPOINT
  }

  @Getter
  @Setter
  public static class BypassProperties {
    private boolean enabled = true;
    private List<String> superAdminRoles = new ArrayList<>();
    private List<String> systemIntegrationRoles = new ArrayList<>();
    private boolean auditEnabled = true;

    void applyDefaults() {
      if (superAdminRoles == null || superAdminRoles.isEmpty()) {
        superAdminRoles = new ArrayList<>(List.of("SUPER_ADMIN", "ROLE_SUPER_ADMIN"));
      } else {
        superAdminRoles = normalise(superAdminRoles);
      }
      if (systemIntegrationRoles == null || systemIntegrationRoles.isEmpty()) {
        systemIntegrationRoles = new ArrayList<>(List.of("ROLE_SERVICE", "ROLE_SYSTEM_INTEGRATION"));
      } else {
        systemIntegrationRoles = normalise(systemIntegrationRoles);
      }
    }

    private List<String> normalise(List<String> roles) {
      return roles.stream()
          .filter(Objects::nonNull)
          .map(role -> role.trim().toUpperCase(Locale.ROOT))
          .distinct()
          .collect(Collectors.toCollection(ArrayList::new));
    }
  }

  @Getter
  @Setter
  public static class DynamicProperties {
    private String subscriptionChannel = "ratelimit:subscription-updates";

    void applyDefaults() {
      if (!StringUtils.hasText(subscriptionChannel)) {
        subscriptionChannel = "ratelimit:subscription-updates";
      }
    }
  }

  /**
   * Provides backward compatible access to the primary key strategy configured for
   * rate limiting. The strategy is derived from the first enabled multidimensional
   * strategy if present, otherwise defaults to {@code tenant}.
   */
  public String getKeyStrategy() {
    List<StrategyProperties> strategies = (multidimensional != null)
        ? multidimensional.getStrategies()
        : List.of();
    if (strategies == null || strategies.isEmpty()) {
      return "tenant";
    }
    return strategies.stream()
        .filter(Objects::nonNull)
        .filter(StrategyProperties::isEnabled)
        .map(StrategyProperties::getName)
        .filter(StringUtils::hasText)
        .map(name -> name.trim().toLowerCase(Locale.ROOT))
        .findFirst()
        .orElse("tenant");
  }

  /**
   * Legacy accessor returning the default rate limit capacity which maps to the
   * request-per-minute quota of the default tier.
   */
  public int getCapacity() {
    TierProperties tier = tier(defaultTier);
    return Math.max(1, tier.getRequestsPerMinute());
  }

  /**
   * Returns the configured algorithm identifier. The new configuration no longer
   * exposes the concept directly, so we default to {@code fixed} for backwards
   * compatibility.
   */
  public String getAlgorithm() {
    return "fixed";
  }
}
