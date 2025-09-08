package com.ejada.billing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_item",
       indexes = @Index(name = "idx_invoice_item_invoice", columnList = "invoice_id,line_no"))
@DynamicUpdate
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "invoice_item_id", updatable = false, nullable = false)
  private Long invoiceItemId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @Column(name = "line_no", nullable = false)
  private Integer lineNo;

  @Column(name = "item_cd", length = 64, nullable = false)
  private String itemCd; // FEATURE/ADDON/USAGE/etc.

  @Column(name = "item_desc", length = 256)
  private String itemDesc;

  @Column(name = "qty", precision = 18, scale = 4, nullable = false)
  private BigDecimal qty;

  @Column(name = "unit_price", precision = 18, scale = 6, nullable = false)
  private BigDecimal unitPrice;

  @Column(name = "line_total", precision = 18, scale = 4, nullable = false)
  private BigDecimal lineTotal;

  public static InvoiceItem of(Invoice inv, int lineNo, String code, String desc,
                               BigDecimal qty, BigDecimal price) {
    BigDecimal total = (qty == null ? BigDecimal.ONE : qty)
        .multiply(price == null ? BigDecimal.ZERO : price);
    return InvoiceItem.builder()
        .invoice(inv).lineNo(lineNo).itemCd(code).itemDesc(desc)
        .qty(qty == null ? BigDecimal.ONE : qty)
        .unitPrice(price == null ? BigDecimal.ZERO : price)
        .lineTotal(total)
        .build();
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Invoice getInvoice() {
    return invoice;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setInvoice(Invoice invoice) {
    this.invoice = invoice;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public static class InvoiceItemBuilder {
    public InvoiceItemBuilder invoice(Invoice invoice) {
      this.invoice = invoice;
      return this;
    }
  }
}
