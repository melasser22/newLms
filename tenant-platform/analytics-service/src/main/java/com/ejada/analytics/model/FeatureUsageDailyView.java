package com.ejada.analytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "mv_tenant_feature_usage_daily")
@Immutable
public class FeatureUsageDailyView {

  @EmbeddedId private FeatureUsageDailyViewId id;

  @Column(name = "event_count")
  private Long eventCount;

  @Column(name = "total_usage")
  private BigDecimal totalUsage;

  @Column(name = "plan_limit")
  private BigDecimal planLimit;

  public FeatureUsageDailyViewId getId() {
    if (id == null) {
      return null;
    }
    return new FeatureUsageDailyViewId(id.getTenantId(), id.getFeatureKey(), id.getUsageDay());
  }

  public Long getEventCount() {
    return eventCount;
  }

  public BigDecimal getTotalUsage() {
    return totalUsage;
  }

  public BigDecimal getPlanLimit() {
    return planLimit;
  }

  public OffsetDateTime getUsageDay() {
    return id == null ? null : id.getUsageDay();
  }
}
