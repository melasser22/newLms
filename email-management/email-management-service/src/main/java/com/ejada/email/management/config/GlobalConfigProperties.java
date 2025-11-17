package com.ejada.email.management.config;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "global-config")
public class GlobalConfigProperties {

  private Map<String, String> sharedSettings = new HashMap<>();
  private Map<String, Boolean> featureFlags = new HashMap<>();
  private Map<String, Map<String, String>> tenantSettings = new HashMap<>();

  public Map<String, String> getSharedSettings() {
    return Map.copyOf(sharedSettings);
  }

  public void setSharedSettings(Map<String, String> sharedSettings) {
    this.sharedSettings = new HashMap<>(sharedSettings);
  }

  public Map<String, Boolean> getFeatureFlags() {
    return Map.copyOf(featureFlags);
  }

  public void setFeatureFlags(Map<String, Boolean> featureFlags) {
    this.featureFlags = new HashMap<>(featureFlags);
  }

  public Map<String, Map<String, String>> getTenantSettings() {
    return tenantSettings.entrySet().stream()
        .collect(
            Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
  }

  public void setTenantSettings(Map<String, Map<String, String>> tenantSettings) {
    this.tenantSettings = tenantSettings.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> new HashMap<>(entry.getValue()),
                (existing, replacement) -> replacement,
                HashMap::new));
  }
}
