package com.ejada.billing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "usage_counter",
       uniqueConstraints = @UniqueConstraint(name = "uk_counter_sub_typ",
                                             columnNames = {"ext_subscription_id", "consumption_typ_cd"}),
       indexes = {
         @Index(name = "idx_usage_counter_sub_typ", columnList = "ext_subscription_id,consumption_typ_cd"),
         @Index(name = "idx_usage_counter_updated_at", columnList = "updated_at DESC")
       })
@DynamicUpdate
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public final class UsageCounter {

  private static final int TYPE_CD_LENGTH = 32;
  private static final int AMOUNT_PRECISION = 18;
  private static final int AMOUNT_SCALE = 4;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "usage_counter_id", updatable = false, nullable = false)
  private Long usageCounterId;

  @Column(name = "ext_subscription_id", nullable = false)
  private Long extSubscriptionId;

  @Column(name = "ext_customer_id", nullable = false)
  private Long extCustomerId;

  /** TRANSACTION | USER | BALANCE (per Swagger). */
  @Column(name = "consumption_typ_cd", length = TYPE_CD_LENGTH, nullable = false)
  private String consumptionTypCd;

  /** For TRANSACTION/USER types. */
  @Column(name = "current_consumption")
  private Long currentConsumption;

  /** For BALANCE type. */
  @Column(name = "current_consumed_amt", precision = AMOUNT_PRECISION, scale = AMOUNT_SCALE)
  private BigDecimal currentConsumedAmt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  @PreUpdate
  void touch() {
    updatedAt = OffsetDateTime.now();
  }

  /** Lightweight reference helper (no DB hit). */
  public static UsageCounter ref(final Long id) {
    UsageCounter c = new UsageCounter();
    c.setUsageCounterId(id);
    return c;
  }
}
