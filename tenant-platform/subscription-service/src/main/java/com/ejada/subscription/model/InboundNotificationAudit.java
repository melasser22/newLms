package com.ejada.subscription.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "inbound_notification_audit",
    uniqueConstraints = @UniqueConstraint(name = "ux_inb_rquid", columnNames = {"rq_uid", "endpoint"})
)
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@SuppressWarnings("checkstyle:MagicNumber")
public class InboundNotificationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inbound_notification_audit_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long inboundNotificationAuditId;

    @Column(name = "rq_uid", nullable = false)
    private UUID rqUid;

    @Column(name = "token_hash", length = 128)
    private String tokenHash;

    @Column(name = "endpoint", length = 64, nullable = false)
    private String endpoint; // RECEIVE_NOTIFICATION | RECEIVE_UPDATE

    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt = OffsetDateTime.now();

    @Column(name = "processed", nullable = false)
    private Boolean processed = Boolean.FALSE;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "status_code", length = 16)
    private String statusCode; // I000000 | EINT000

    @Column(name = "status_desc", length = 64)
    private String statusDesc;

    @Column(name = "status_dtls", columnDefinition = "jsonb")
    private String statusDtls;

    public static InboundNotificationAudit ref(final Long id) {
        if (id == null) {
            return null;
        }
        InboundNotificationAudit x = new InboundNotificationAudit();
        x.setInboundNotificationAuditId(id);
        return x;
    }
}
