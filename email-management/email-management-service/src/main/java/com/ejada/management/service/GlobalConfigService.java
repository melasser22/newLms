package com.ejada.management.service;

import com.ejada.management.config.GlobalConfigProperties;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GlobalConfigService {

  private final GlobalConfigProperties properties;

  public GlobalConfigService(GlobalConfigProperties properties) {
    this.properties = properties;
  }

  public boolean isFeatureEnabled(String feature) {
    return properties.getFeatureFlags().getOrDefault(feature, Boolean.TRUE);
  }

  public Map<String, String> tenantSettings(String tenantId) {
    Map<String, String> merged = new HashMap<>(properties.getSharedSettings());
    Map<String, String> overrides = properties.getTenantSettings().get(tenantId);
    if (overrides != null) {
      merged.putAll(overrides);
    }
    return merged;
  }
}
