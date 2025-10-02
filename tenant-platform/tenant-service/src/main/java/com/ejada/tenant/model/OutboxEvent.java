package com.ejada.tenant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_event",
        indexes = {
                @Index(name = "idx_outbox_unpublished_tenant", columnList = "published, created_at")
        })
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    private static final int AGGREGATE_TYPE_LENGTH = 64;
    private static final int AGGREGATE_ID_LENGTH = 128;
    private static final int EVENT_TYPE_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_event_id", updatable = false, nullable = false)
    private Long outboxEventId;

    @Column(name = "aggregate_type", length = AGGREGATE_TYPE_LENGTH, nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", length = AGGREGATE_ID_LENGTH, nullable = false)
    private String aggregateId;

    @Column(name = "event_type", length = EVENT_TYPE_LENGTH, nullable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published", nullable = false)
    private Boolean published;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (published == null) {
            published = Boolean.FALSE;
        }
    }
}
