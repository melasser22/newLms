package com.ejada.analytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "mv_tenant_usage_summary")
@Immutable
public class UsageSummaryView {

  @EmbeddedId private UsageSummaryViewId id;

  @Column(name = "event_count")
  private Long eventCount;

  @Column(name = "total_usage")
  private BigDecimal totalUsage;

  @Column(name = "plan_limit")
  private BigDecimal planLimit;

  @Column(name = "last_event_at")
  private OffsetDateTime lastEventAt;

  public UsageSummaryViewId getId() {
    return id;
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

  public OffsetDateTime getLastEventAt() {
    return lastEventAt;
  }
}
