package com.ejada.billing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.OffsetDateTime;

@Entity
@Table(name = "invoice_attachment",
       indexes = @Index(name = "idx_invoice_attachment_invoice_created",
                        columnList = "invoice_id,created_at DESC"))
@DynamicUpdate
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public final class InvoiceAttachment {

  private static final int FILE_NM_LENGTH = 255;
  private static final int MIME_TYP_LENGTH = 128;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "invoice_attachment_id", updatable = false, nullable = false)
  private Long invoiceAttachmentId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @Column(name = "file_nm", length = FILE_NM_LENGTH, nullable = false)
  private String fileNm;

  @Column(name = "mime_typ", length = MIME_TYP_LENGTH, nullable = false)
  private String mimeTyp;

  @Lob
  @Column(name = "content", nullable = false)
  private byte[] content;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void onInsert() {
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Invoice is a JPA entity; reference sharing is intentional")
  @Builder
  public InvoiceAttachment(Long invoiceAttachmentId, Invoice invoice, String fileNm,
                           String mimeTyp, byte[] content, OffsetDateTime createdAt) {
    this.invoiceAttachmentId = invoiceAttachmentId;
    this.invoice = invoice;
    this.fileNm = fileNm;
    this.mimeTyp = mimeTyp;
    this.content = content == null ? null : content.clone();
    this.createdAt = createdAt;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Invoice getInvoice() {
    return invoice;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setInvoice(final Invoice invoice) {
    this.invoice = invoice;
  }

  public byte[] getContent() {
    return content == null ? null : content.clone();
  }

  public void setContent(final byte[] content) {
    this.content = content == null ? null : content.clone();
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public static final class InvoiceAttachmentBuilder {
    public InvoiceAttachmentBuilder invoice(final Invoice invoice) {
      this.invoice = invoice;
      return this;
    }

    public InvoiceAttachmentBuilder content(final byte[] content) {
      this.content = content == null ? null : content.clone();
      return this;
    }
  }
}
