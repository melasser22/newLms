package com.ejada.gateway.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for gateway response caching.
 */
@ConfigurationProperties(prefix = "gateway.cache")
public class GatewayCacheProperties {

  private boolean enabled;

  private Map<String, Duration> ttlMap = new LinkedHashMap<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Map<String, Duration> getTtlMap() {
    return ttlMap;
  }

  public void setTtlMap(Map<String, Duration> ttlMap) {
    LinkedHashMap<String, Duration> copy = new LinkedHashMap<>();
    if (ttlMap != null) {
      ttlMap.forEach((key, value) -> {
        if (!StringUtils.hasText(key) || value == null) {
          return;
        }
        copy.put(key.trim(), value);
      });
    }
    this.ttlMap = copy;
  }

  public Duration resolveTtl(String routeId) {
    if (!StringUtils.hasText(routeId)) {
      return null;
    }
    return ttlMap.get(routeId);
  }
}

