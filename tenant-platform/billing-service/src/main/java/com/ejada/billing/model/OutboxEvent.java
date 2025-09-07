package com.ejada.billing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

@Entity
@Table(name = "outbox_event",
       indexes = {
         @Index(name = "idx_outbox_unpublished", columnList = "published,created_at")
       })
@DynamicUpdate
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OutboxEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "outbox_event_id", updatable = false, nullable = false)
  private Long outboxEventId;

  @Column(name = "aggregate_type", length = 64, nullable = false)
  private String aggregateType;

  @Column(name = "aggregate_id", length = 128, nullable = false)
  private String aggregateId;

  @Column(name = "event_type", length = 64, nullable = false)
  private String eventType;

  @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
  private String payload;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "published", nullable = false)
  private Boolean published;

  @Column(name = "published_at")
  private OffsetDateTime publishedAt;

  @PrePersist
  void preInsert() {
    if (createdAt == null) createdAt = OffsetDateTime.now();
    if (published == null) published = Boolean.FALSE;
  }

  public static OutboxEvent ref(Long id) {
    OutboxEvent e = new OutboxEvent();
    e.setOutboxEventId(id);
    return e;
  }
}
