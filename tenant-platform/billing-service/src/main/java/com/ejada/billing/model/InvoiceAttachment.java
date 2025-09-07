package com.ejada.billing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

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
}
