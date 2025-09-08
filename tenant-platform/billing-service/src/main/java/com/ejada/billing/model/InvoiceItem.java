package com.ejada.billing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_item",
       indexes = @Index(name = "idx_invoice_item_invoice", columnList = "invoice_id,line_no"))
@DynamicUpdate
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public final class InvoiceItem {

  private static final int ITEM_CD_LENGTH = 64;
  private static final int ITEM_DESC_LENGTH = 256;
  private static final int AMOUNT_PRECISION = 18;
  private static final int SCALE_FOUR = 4;
  private static final int SCALE_SIX = 6;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "invoice_item_id", updatable = false, nullable = false)
  private Long invoiceItemId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @Column(name = "line_no", nullable = false)
  private Integer lineNo;

  @Column(name = "item_cd", length = ITEM_CD_LENGTH, nullable = false)
  private String itemCd; // FEATURE/ADDON/USAGE/etc.

  @Column(name = "item_desc", length = ITEM_DESC_LENGTH)
  private String itemDesc;

  @Column(name = "qty", precision = AMOUNT_PRECISION, scale = SCALE_FOUR, nullable = false)
  private BigDecimal qty;

  @Column(name = "unit_price", precision = AMOUNT_PRECISION, scale = SCALE_SIX, nullable = false)
  private BigDecimal unitPrice;

  @Column(name = "line_total", precision = AMOUNT_PRECISION, scale = SCALE_FOUR, nullable = false)
  private BigDecimal lineTotal;

  public static InvoiceItem of(final Invoice inv, final int lineNo, final String code, final String desc,
                               final BigDecimal qty, final BigDecimal price) {
    BigDecimal total = (qty == null ? BigDecimal.ONE : qty)
        .multiply(price == null ? BigDecimal.ZERO : price);
    return InvoiceItem.builder()
        .invoice(inv).lineNo(lineNo).itemCd(code).itemDesc(desc)
        .qty(qty == null ? BigDecimal.ONE : qty)
        .unitPrice(price == null ? BigDecimal.ZERO : price)
        .lineTotal(total)
        .build();
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Invoice is a JPA entity; reference sharing is intentional")
  @Builder
  public InvoiceItem(Long invoiceItemId, Invoice invoice, Integer lineNo, String itemCd,
                     String itemDesc, BigDecimal qty, BigDecimal unitPrice, BigDecimal lineTotal) {
    this.invoiceItemId = invoiceItemId;
    this.invoice = invoice;
    this.lineNo = lineNo;
    this.itemCd = itemCd;
    this.itemDesc = itemDesc;
    this.qty = qty;
    this.unitPrice = unitPrice;
    this.lineTotal = lineTotal;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Invoice getInvoice() {
    return invoice;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setInvoice(final Invoice invoice) {
    this.invoice = invoice;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public static final class InvoiceItemBuilder {
    public InvoiceItemBuilder invoice(final Invoice invoice) {
      this.invoice = invoice;
      return this;
    }
  }
}
