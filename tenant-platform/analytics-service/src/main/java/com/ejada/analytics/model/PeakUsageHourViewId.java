package com.ejada.analytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Embeddable
public class PeakUsageHourViewId implements Serializable {

  @Column(name = "tenant_id")
  private Long tenantId;

  @Column(name = "feature_key")
  private String featureKey;

  @Column(name = "usage_hour")
  private OffsetDateTime usageHour;

  public PeakUsageHourViewId() {}

  public PeakUsageHourViewId(Long tenantId, String featureKey, OffsetDateTime usageHour) {
    this.tenantId = tenantId;
    this.featureKey = featureKey;
    this.usageHour = usageHour;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public String getFeatureKey() {
    return featureKey;
  }

  public OffsetDateTime getUsageHour() {
    return usageHour;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PeakUsageHourViewId that = (PeakUsageHourViewId) o;
    return Objects.equals(tenantId, that.tenantId)
        && Objects.equals(featureKey, that.featureKey)
        && Objects.equals(usageHour, that.usageHour);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantId, featureKey, usageHour);
  }
}
