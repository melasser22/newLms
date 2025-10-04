package com.ejada.gateway.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for multi-region disaster recovery strategies.
 */
@Validated
@ConfigurationProperties(prefix = "gateway.dr")
public class GatewayDisasterRecoveryProperties {

  private boolean enabled = false;
  private String primaryRegion = System.getenv().getOrDefault("PRIMARY_REGION", "primary");
  private List<String> backupRegions = new ArrayList<>();
  private int failureThreshold = 5;
  private Duration failureWindow = Duration.ofSeconds(30);
  private Duration recoveryWindow = Duration.ofMinutes(5);

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getPrimaryRegion() {
    return primaryRegion;
  }

  public void setPrimaryRegion(String primaryRegion) {
    this.primaryRegion = primaryRegion;
  }

  public List<String> getBackupRegions() {
    return backupRegions;
  }

  public void setBackupRegions(List<String> backupRegions) {
    this.backupRegions = (backupRegions != null) ? new ArrayList<>(backupRegions) : new ArrayList<>();
  }

  public int getFailureThreshold() {
    return failureThreshold;
  }

  public void setFailureThreshold(int failureThreshold) {
    this.failureThreshold = failureThreshold;
  }

  public Duration getFailureWindow() {
    return failureWindow;
  }

  public void setFailureWindow(Duration failureWindow) {
    this.failureWindow = failureWindow;
  }

  public Duration getRecoveryWindow() {
    return recoveryWindow;
  }

  public void setRecoveryWindow(Duration recoveryWindow) {
    this.recoveryWindow = recoveryWindow;
  }
}
