
package com.ejada.actuator.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shared.actuator")
public class SharedActuatorProperties {

  private final HttpExchanges httpExchanges = new HttpExchanges();
  private final MetricsTags metrics = new MetricsTags();
  private final Security security = new Security();
  private final SlaReport slaReport = new SlaReport();

  public HttpExchanges getHttpExchanges() { return httpExchanges; }
  public MetricsTags getMetrics() { return metrics; }
  public Security getSecurity() { return security; }
  public SlaReport getSlaReport() { return slaReport; }

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

  public static class SlaReport {
    private boolean enabled = true;
    private String owner;
    private String contact;
    private String description;
    private boolean slaCompliant = true;
    private double availabilityPercent = 99.95d;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isSlaCompliant() { return slaCompliant; }
    public void setSlaCompliant(boolean slaCompliant) { this.slaCompliant = slaCompliant; }
    public double getAvailabilityPercent() { return availabilityPercent; }
    public void setAvailabilityPercent(double availabilityPercent) { this.availabilityPercent = availabilityPercent; }
  }
}
