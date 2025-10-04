package com.ejada.gateway.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Configuration properties controlling the subscription validation filter.
 */
@ConfigurationProperties(prefix = "gateway.subscription")
public class SubscriptionValidationProperties {

  /** Enable or disable subscription validation. */
  private boolean enabled = true;

  /** URI template for subscription validation endpoint (supports {tenantId}). */
  private String validationUri = "lb://subscription-service/internal/subscriptions/{tenantId}";

  /** Cache time-to-live for subscription lookups. */
  private Duration cacheTtl = Duration.ofMinutes(5);

  /** Grace period duration after subscription expiry during which read-only access is allowed. */
  private Duration gracePeriod = Duration.ofDays(7);

  /** Cache key prefix. */
  private String cachePrefix = "gateway:subscription:";

  /** Whether to allow traffic to continue when validation fails. */
  private boolean failOpen = true;

  /** Apply validation to all routes (otherwise only those listed in requiredFeatures). */
  private boolean validateAllRoutes = true;

  /** Ant-style patterns to skip subscription validation (e.g. /actuator/**). */
  private String[] skipPatterns = new String[] {"/actuator/**", "/fallback/**"};

  /** Optional mapping of routeId to required feature identifier. */
  private Map<String, String> requiredFeatures = new LinkedHashMap<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getValidationUri() {
    return validationUri;
  }

  public void setValidationUri(String validationUri) {
    this.validationUri = validationUri;
  }

  public Duration getCacheTtl() {
    return cacheTtl;
  }

  public void setCacheTtl(Duration cacheTtl) {
    this.cacheTtl = cacheTtl;
  }

  public Duration getGracePeriod() {
    return gracePeriod;
  }

  public void setGracePeriod(Duration gracePeriod) {
    if (gracePeriod == null || gracePeriod.isNegative()) {
      this.gracePeriod = Duration.ZERO;
    } else {
      this.gracePeriod = gracePeriod;
    }
  }

  public String getCachePrefix() {
    return cachePrefix;
  }

  public void setCachePrefix(String cachePrefix) {
    this.cachePrefix = cachePrefix;
  }

  public boolean isFailOpen() {
    return failOpen;
  }

  public void setFailOpen(boolean failOpen) {
    this.failOpen = failOpen;
  }

  public boolean isValidateAllRoutes() {
    return validateAllRoutes;
  }

  public void setValidateAllRoutes(boolean validateAllRoutes) {
    this.validateAllRoutes = validateAllRoutes;
  }

  public String[] getSkipPatterns() {
    return skipPatterns;
  }

  public void setSkipPatterns(String[] skipPatterns) {
    this.skipPatterns = (skipPatterns != null) ? skipPatterns : new String[0];
  }

  public Map<String, String> getRequiredFeatures() {
    return requiredFeatures;
  }

  public void setRequiredFeatures(Map<String, String> requiredFeatures) {
    this.requiredFeatures = (requiredFeatures != null)
        ? new LinkedHashMap<>(requiredFeatures)
        : new LinkedHashMap<>();
  }

  public boolean requiresValidation(String routeId) {
    if (validateAllRoutes) {
      return true;
    }
    return StringUtils.hasText(routeId) && requiredFeatures.containsKey(routeId);
  }

  public Set<String> requiredFeaturesFor(String routeId) {
    if (!StringUtils.hasText(routeId)) {
      return Collections.emptySet();
    }
    String value = requiredFeatures.get(routeId);
    if (!StringUtils.hasText(value)) {
      return Collections.emptySet();
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public String cacheKey(String tenantId, @Nullable String feature) {
    if (StringUtils.hasText(feature)) {
      return cachePrefix + tenantId + ":" + feature;
    }
    return cachePrefix + tenantId;
  }
}

