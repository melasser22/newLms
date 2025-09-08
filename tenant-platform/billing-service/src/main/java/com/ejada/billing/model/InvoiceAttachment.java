package com.ejada.billing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.OffsetDateTime;

@Entity
@Table(name = "invoice_attachment",
       indexes = @Index(name = "idx_invoice_attachment_invoice_created",
                        columnList = "invoice_id,created_at DESC"))
@DynamicUpdate
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "invoice_attachment_id", updatable = false, nullable = false)
  private Long invoiceAttachmentId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @Column(name = "file_nm", length = 255, nullable = false)
  private String fileNm;

  @Column(name = "mime_typ", length = 128, nullable = false)
  private String mimeTyp;

  @Lob
  @Column(name = "content", nullable = false)
  private byte[] content;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void onInsert() {
    if (createdAt == null) createdAt = OffsetDateTime.now();
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Invoice getInvoice() {
    return invoice;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setInvoice(Invoice invoice) {
    this.invoice = invoice;
  }

  public byte[] getContent() {
    return content == null ? null : content.clone();
  }

  public void setContent(byte[] content) {
    this.content = content == null ? null : content.clone();
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public static class InvoiceAttachmentBuilder {
    public InvoiceAttachmentBuilder invoice(Invoice invoice) {
      this.invoice = invoice;
      return this;
    }

    public InvoiceAttachmentBuilder content(byte[] content) {
      this.content = content == null ? null : content.clone();
      return this;
    }
  }
}
