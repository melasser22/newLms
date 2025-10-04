package com.ejada.gateway.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration flags governing structured access logging for the gateway.
 */
@ConfigurationProperties(prefix = "gateway.logging")
public class GatewayLoggingProperties {

  private AccessLog accessLog = new AccessLog();

  public AccessLog getAccessLog() {
    return accessLog;
  }

  public void setAccessLog(AccessLog accessLog) {
    this.accessLog = accessLog == null ? new AccessLog() : accessLog;
  }

  public static class AccessLog {

    private boolean enabled;
    private List<String> skipPatterns = new ArrayList<>(List.of("/actuator/**", "/health"));

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public List<String> getSkipPatterns() {
      return skipPatterns;
    }

    public void setSkipPatterns(List<String> skipPatterns) {
      if (skipPatterns == null) {
        this.skipPatterns = new ArrayList<>();
        return;
      }
      List<String> sanitized = new ArrayList<>();
      for (String pattern : skipPatterns) {
        if (StringUtils.hasText(pattern)) {
          sanitized.add(pattern.trim());
        }
      }
      this.skipPatterns = sanitized;
    }
  }
}
