package com.ejada.email.usage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "tenant_quota",
    indexes = {@Index(name = "idx_quota_tenant", columnList = "tenantId", unique = true)})
public class TenantQuota {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private long monthlyQuota;

  @Column(nullable = false)
  private long dailyBurstLimit;

  @Column(nullable = false)
  private int alertThresholdPercent;

  @Column(nullable = false)
  private Instant updatedAt;

  protected TenantQuota() {}

  public TenantQuota(
      Long id,
      String tenantId,
      long monthlyQuota,
      long dailyBurstLimit,
      int alertThresholdPercent,
      Instant updatedAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.monthlyQuota = monthlyQuota;
    this.dailyBurstLimit = dailyBurstLimit;
    this.alertThresholdPercent = alertThresholdPercent;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public long getMonthlyQuota() {
    return monthlyQuota;
  }

  public long getDailyBurstLimit() {
    return dailyBurstLimit;
  }

  public int getAlertThresholdPercent() {
    return alertThresholdPercent;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TenantQuota that = (TenantQuota) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
