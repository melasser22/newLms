package com.ejada.billing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usage_event",
       indexes = {
         @Index(name = "idx_usage_event_product_time", columnList = "ext_product_id,received_at DESC"),
         @Index(name = "idx_usage_event_rq_uid", columnList = "rq_uid")
       })
@DynamicUpdate
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UsageEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "usage_event_id", updatable = false, nullable = false)
  private Long usageEventId;

  @Column(name = "rq_uid", nullable = false)
  private UUID rqUid;

  @Column(name = "token_hash", length = 64)
  private String tokenHash;

  /** Raw request payload for audit (JSON string stored in jsonb). */
  @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
  private String payload;

  @Column(name = "ext_product_id", nullable = false)
  private Long extProductId;

  @Column(name = "received_at", nullable = false)
  private OffsetDateTime receivedAt;

  @Column(name = "processed", nullable = false)
  private Boolean processed;

  @Column(name = "status_code", length = 32, nullable = false)
  private String statusCode;

  @Column(name = "status_desc", length = 128, nullable = false)
  private String statusDesc;

  /** Optional details (errors etc.) as JSON. */
  @Column(name = "status_dtls", columnDefinition = "jsonb")
  private String statusDtls;

  @PrePersist
  void onInsert() {
    if (receivedAt == null) receivedAt = OffsetDateTime.now();
    if (processed == null) processed = Boolean.TRUE;
  }

  public static UsageEvent ref(Long id) {
    UsageEvent e = new UsageEvent();
    e.setUsageEventId(id);
    return e;
  }
}
