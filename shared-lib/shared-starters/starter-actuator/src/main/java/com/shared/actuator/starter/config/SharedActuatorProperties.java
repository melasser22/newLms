
package com.shared.actuator.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shared.actuator")
public class SharedActuatorProperties {

  private final HttpExchanges httpExchanges = new HttpExchanges();
  private final MetricsTags metrics = new MetricsTags();
  private final Security security = new Security();

  public HttpExchanges getHttpExchanges() { return httpExchanges; }
  public MetricsTags getMetrics() { return metrics; }
  public Security getSecurity() { return security; }

  public static class HttpExchanges {
    private boolean enabled = true;
    private int capacity = 500;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
  }

  public static class MetricsTags {
    private final CommonTags commonTags = new CommonTags();
    public CommonTags getCommonTags() { return commonTags; }
    public static class CommonTags {
      private boolean applicationEnabled = true;
      private boolean environmentEnabled = true;
      private boolean regionEnabled = true;
      private boolean zoneEnabled = true;
      public boolean isApplicationEnabled() { return applicationEnabled; }
      public void setApplicationEnabled(boolean applicationEnabled) { this.applicationEnabled = applicationEnabled; }
      public boolean isEnvironmentEnabled() { return environmentEnabled; }
      public void setEnvironmentEnabled(boolean environmentEnabled) { this.environmentEnabled = environmentEnabled; }
      public boolean isRegionEnabled() { return regionEnabled; }
      public void setRegionEnabled(boolean regionEnabled) { this.regionEnabled = regionEnabled; }
      public boolean isZoneEnabled() { return zoneEnabled; }
      public void setZoneEnabled(boolean zoneEnabled) { this.zoneEnabled = zoneEnabled; }
    }
  }

  public static class Security {
    private boolean enabled = false;
    private boolean permitPrometheus = true;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isPermitPrometheus() { return permitPrometheus; }
    public void setPermitPrometheus(boolean permitPrometheus) { this.permitPrometheus = permitPrometheus; }
  }
}
