package com.ejada.analytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Embeddable
public class FeatureUsageDailyViewId implements Serializable {

  @Column(name = "tenant_id")
  private Long tenantId;

  @Column(name = "feature_key")
  private String featureKey;

  @Column(name = "usage_day")
  private OffsetDateTime usageDay;

  public FeatureUsageDailyViewId() {}

  public FeatureUsageDailyViewId(Long tenantId, String featureKey, OffsetDateTime usageDay) {
    this.tenantId = tenantId;
    this.featureKey = featureKey;
    this.usageDay = usageDay;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getFeatureKey() {
    return featureKey;
  }

  public void setFeatureKey(String featureKey) {
    this.featureKey = featureKey;
  }

  public OffsetDateTime getUsageDay() {
    return usageDay;
  }

  public void setUsageDay(OffsetDateTime usageDay) {
    this.usageDay = usageDay;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeatureUsageDailyViewId that = (FeatureUsageDailyViewId) o;
    return Objects.equals(tenantId, that.tenantId)
        && Objects.equals(featureKey, that.featureKey)
        && Objects.equals(usageDay, that.usageDay);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantId, featureKey, usageDay);
  }
}
