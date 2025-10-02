package com.ejada.analytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "mv_tenant_peak_usage_hourly")
@Immutable
public class PeakUsageHourView {

  @EmbeddedId private PeakUsageHourViewId id;

  @Column(name = "event_count")
  private Long eventCount;

  @Column(name = "total_usage")
  private BigDecimal totalUsage;

  public PeakUsageHourViewId getId() {
    return id;
  }

  public Long getEventCount() {
    return eventCount;
  }

  public BigDecimal getTotalUsage() {
    return totalUsage;
  }
}
