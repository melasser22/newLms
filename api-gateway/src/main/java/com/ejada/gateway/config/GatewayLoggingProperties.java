package com.ejada.gateway.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Logging configuration dedicated to the gateway runtime.
 */
@ConfigurationProperties(prefix = "gateway.logging")
public class GatewayLoggingProperties {

  private final AccessLog accessLog = new AccessLog();

  public AccessLog getAccessLog() {
    return accessLog;
  }

  public static class AccessLog {

    private boolean enabled = false;
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
      } else {
        this.skipPatterns = new ArrayList<>(skipPatterns);
      }
    }
  }
}
