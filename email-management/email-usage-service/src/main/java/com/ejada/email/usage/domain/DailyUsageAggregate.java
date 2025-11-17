package com.ejada.email.usage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(
    name = "email_daily_usage",
    indexes = {
      @Index(name = "idx_usage_tenant_date", columnList = "tenantId,usageDate", unique = true),
      @Index(name = "idx_usage_date", columnList = "usageDate")
    })
public class DailyUsageAggregate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private LocalDate usageDate;

  @Column(nullable = false)
  private long sentCount;

  @Column(nullable = false)
  private long deliveredCount;

  @Column(nullable = false)
  private long bouncedCount;

  @Column(nullable = false)
  private long openedCount;

  @Column(nullable = false)
  private long clickedCount;

  @Column(nullable = false)
  private long spamComplaintCount;

  @Column(nullable = false)
  private long deferredCount;

  @Column(nullable = false)
  private long blockedCount;

  @Column(nullable = false)
  private long quotaConsumed;

  protected DailyUsageAggregate() {}

  public DailyUsageAggregate(
      Long id,
      String tenantId,
      LocalDate usageDate,
      long sentCount,
      long deliveredCount,
      long bouncedCount,
      long openedCount,
      long clickedCount,
      long spamComplaintCount,
      long deferredCount,
      long blockedCount,
      long quotaConsumed) {
    this.id = id;
    this.tenantId = tenantId;
    this.usageDate = usageDate;
    this.sentCount = sentCount;
    this.deliveredCount = deliveredCount;
    this.bouncedCount = bouncedCount;
    this.openedCount = openedCount;
    this.clickedCount = clickedCount;
    this.spamComplaintCount = spamComplaintCount;
    this.deferredCount = deferredCount;
    this.blockedCount = blockedCount;
    this.quotaConsumed = quotaConsumed;
  }

  public Long getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public LocalDate getUsageDate() {
    return usageDate;
  }

  public long getSentCount() {
    return sentCount;
  }

  public long getDeliveredCount() {
    return deliveredCount;
  }

  public long getBouncedCount() {
    return bouncedCount;
  }

  public long getOpenedCount() {
    return openedCount;
  }

  public long getClickedCount() {
    return clickedCount;
  }

  public long getSpamComplaintCount() {
    return spamComplaintCount;
  }

  public long getDeferredCount() {
    return deferredCount;
  }

  public long getBlockedCount() {
    return blockedCount;
  }

  public long getQuotaConsumed() {
    return quotaConsumed;
  }

  public DailyUsageAggregate withQuotaConsumed(long quotaConsumed) {
    return new DailyUsageAggregate(
        id,
        tenantId,
        usageDate,
        sentCount,
        deliveredCount,
        bouncedCount,
        openedCount,
        clickedCount,
        spamComplaintCount,
        deferredCount,
        blockedCount,
        quotaConsumed);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DailyUsageAggregate that = (DailyUsageAggregate) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
