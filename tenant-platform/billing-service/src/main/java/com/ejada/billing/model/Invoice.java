package com.ejada.billing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "invoice",
       indexes = {
         @Index(name = "idx_invoice_sub", columnList = "ext_subscription_id"),
         @Index(name = "idx_invoice_customer", columnList = "ext_customer_id"),
         @Index(name = "idx_invoice_status_dt", columnList = "status_cd,invoice_dt DESC")
       })
@DynamicUpdate
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public final class Invoice {

  private static final int CURRENCY_LENGTH = 3;
  private static final int AMOUNT_PRECISION = 18;
  private static final int AMOUNT_SCALE = 4;
  private static final int STATUS_CD_LENGTH = 32;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "invoice_id", updatable = false, nullable = false)
  private Long invoiceId;

  @Column(name = "ext_subscription_id", nullable = false)
  private Long extSubscriptionId;

  @Column(name = "ext_customer_id", nullable = false)
  private Long extCustomerId;

  @Column(name = "currency", length = CURRENCY_LENGTH, nullable = false)
  private String currency;

  @Column(name = "subtotal_amt", precision = AMOUNT_PRECISION, scale = AMOUNT_SCALE, nullable = false)
  private BigDecimal subtotalAmt;

  @Column(name = "tax_amt", precision = AMOUNT_PRECISION, scale = AMOUNT_SCALE, nullable = false)
  private BigDecimal taxAmt;

  @Column(name = "total_amt", precision = AMOUNT_PRECISION, scale = AMOUNT_SCALE, nullable = false)
  private BigDecimal totalAmt;

  @Column(name = "invoice_dt", nullable = false)
  private LocalDate invoiceDt;

  @Column(name = "due_dt")
  private LocalDate dueDt;

  @Column(name = "status_cd", length = STATUS_CD_LENGTH, nullable = false)
  private String statusCd; // DRAFT | ISSUED | PAID | VOID

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void preInsert() {
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
    if (subtotalAmt == null) {
      subtotalAmt = BigDecimal.ZERO;
    }
    if (taxAmt == null) {
      taxAmt = BigDecimal.ZERO;
    }
    if (totalAmt == null) {
      totalAmt = BigDecimal.ZERO;
    }
    if (statusCd == null) {
      statusCd = "DRAFT";
    }
  }

  public static Invoice ref(final Long id) {
    Invoice i = new Invoice();
    i.setInvoiceId(id);
    return i;
  }
}
