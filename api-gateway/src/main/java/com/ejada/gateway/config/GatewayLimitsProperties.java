package com.ejada.gateway.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for gateway request limits.
 */
@ConfigurationProperties(prefix = "gateway.limits")
public class GatewayLimitsProperties {

  private Map<String, DataSize> maxRequestSize = new LinkedHashMap<>();

  public Map<String, DataSize> getMaxRequestSize() {
    return maxRequestSize;
  }

  public void setMaxRequestSize(Map<String, DataSize> maxRequestSize) {
    LinkedHashMap<String, DataSize> copy = new LinkedHashMap<>();
    if (maxRequestSize != null) {
      maxRequestSize.forEach((key, value) -> {
        if (!StringUtils.hasText(key) || value == null) {
          return;
        }
        copy.put(key.trim(), value);
      });
    }
    this.maxRequestSize = copy;
  }

  public DataSize resolveMaxSize(String routeId) {
    if (!StringUtils.hasText(routeId)) {
      return null;
    }
    return maxRequestSize.get(routeId);
  }
}

