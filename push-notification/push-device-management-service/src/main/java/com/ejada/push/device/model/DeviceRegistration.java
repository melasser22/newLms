package com.ejada.push.device.model;

import java.time.Instant;

public class DeviceRegistration {
  private final String userId;
  private final String deviceToken;
  private final String platform;
  private final String appId;
  private boolean active;
  private Instant lastSeenAt;

  public DeviceRegistration(String userId, String deviceToken, String platform, String appId) {
    this.userId = userId;
    this.deviceToken = deviceToken;
    this.platform = platform;
    this.appId = appId;
    this.active = true;
    this.lastSeenAt = Instant.now();
  }

  public String getUserId() {
    return userId;
  }

  public String getDeviceToken() {
    return deviceToken;
  }

  public String getPlatform() {
    return platform;
  }

  public String getAppId() {
    return appId;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getLastSeenAt() {
    return lastSeenAt;
  }

  public void refresh() {
    this.lastSeenAt = Instant.now();
    this.active = true;
  }

  public void deactivate() {
    this.active = false;
  }
}
