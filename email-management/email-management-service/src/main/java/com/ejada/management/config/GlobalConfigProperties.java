package com.ejada.management.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "global-config")
public class GlobalConfigProperties {

  private Map<String, String> sharedSettings = new HashMap<>();
  private Map<String, Boolean> featureFlags = new HashMap<>();
  private Map<String, Map<String, String>> tenantSettings = new HashMap<>();

  public Map<String, String> getSharedSettings() {
    return sharedSettings;
  }

  public void setSharedSettings(Map<String, String> sharedSettings) {
    this.sharedSettings = sharedSettings;
  }

  public Map<String, Boolean> getFeatureFlags() {
    return featureFlags;
  }

  public void setFeatureFlags(Map<String, Boolean> featureFlags) {
    this.featureFlags = featureFlags;
  }

  public Map<String, Map<String, String>> getTenantSettings() {
    return tenantSettings;
  }

  public void setTenantSettings(Map<String, Map<String, String>> tenantSettings) {
    this.tenantSettings = tenantSettings;
  }
}
