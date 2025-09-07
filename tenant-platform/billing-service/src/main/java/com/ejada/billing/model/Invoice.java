package com.ejada.billing.model;

import jakarta.persistence.*;
import lombok.*;
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
public class Invoice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "invoice_id", updatable = false, nullable = false)
  private Long invoiceId;

  @Column(name = "ext_subscription_id", nullable = false)
  private Long extSubscriptionId;

  @Column(name = "ext_customer_id", nullable = false)
  private Long extCustomerId;

  @Column(name = "currency", length = 3, nullable = false)
  private String currency;

  @Column(name = "subtotal_amt", precision = 18, scale = 4, nullable = false)
  private BigDecimal subtotalAmt;

  @Column(name = "tax_amt", precision = 18, scale = 4, nullable = false)
  private BigDecimal taxAmt;

  @Column(name = "total_amt", precision = 18, scale = 4, nullable = false)
  private BigDecimal totalAmt;

  @Column(name = "invoice_dt", nullable = false)
  private LocalDate invoiceDt;

  @Column(name = "due_dt")
  private LocalDate dueDt;

  @Column(name = "status_cd", length = 32, nullable = false)
  private String statusCd; // DRAFT | ISSUED | PAID | VOID

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void preInsert() {
    if (createdAt == null) createdAt = OffsetDateTime.now();
    if (subtotalAmt == null) subtotalAmt = BigDecimal.ZERO;
    if (taxAmt == null) taxAmt = BigDecimal.ZERO;
    if (totalAmt == null) totalAmt = BigDecimal.ZERO;
    if (statusCd == null) statusCd = "DRAFT";
  }

  public static Invoice ref(Long id) {
    Invoice i = new Invoice();
    i.setInvoiceId(id);
    return i;
  }
}
