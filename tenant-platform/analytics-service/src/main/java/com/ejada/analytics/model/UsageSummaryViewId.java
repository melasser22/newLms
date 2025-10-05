package com.ejada.analytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Embeddable
public class UsageSummaryViewId implements Serializable {

  private static final long serialVersionUID = 1L;

  @Column(name = "tenant_id")
  private Long tenantId;

  @Column(name = "feature_key")
  private String featureKey;

  @Column(name = "usage_period")
  private OffsetDateTime usagePeriod;

  public UsageSummaryViewId() {}

  public UsageSummaryViewId(Long tenantId, String featureKey, OffsetDateTime usagePeriod) {
    this.tenantId = tenantId;
    this.featureKey = featureKey;
    this.usagePeriod = usagePeriod;
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

  public OffsetDateTime getUsagePeriod() {
    return usagePeriod;
  }

  public void setUsagePeriod(OffsetDateTime usagePeriod) {
    this.usagePeriod = usagePeriod;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UsageSummaryViewId that = (UsageSummaryViewId) o;
    return Objects.equals(tenantId, that.tenantId)
        && Objects.equals(featureKey, that.featureKey)
        && Objects.equals(usagePeriod, that.usagePeriod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantId, featureKey, usagePeriod);
  }
}
