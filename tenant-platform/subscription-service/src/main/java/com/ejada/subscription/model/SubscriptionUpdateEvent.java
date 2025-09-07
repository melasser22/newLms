package com.ejada.subscription.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "subscription_update_event",
    uniqueConstraints = @UniqueConstraint(name = "ux_su_unique", columnNames = "rq_uid"),
    indexes = @Index(name = "idx_sue_sub", columnList = "ext_subscription_id")
)
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SubscriptionUpdateEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_update_event_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long subscriptionUpdateEventId;

    @Column(name = "rq_uid", nullable = false)
    private UUID rqUid;

    @Column(name = "ext_subscription_id", nullable = false)
    private Long extSubscriptionId;

    @Column(name = "ext_customer_id", nullable = false)
    private Long extCustomerId;

    @Column(name = "update_type", length = 16, nullable = false)
    private String updateType; // SUSPENDED | RESUMED | TERMINATED | EXPIRED

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt = OffsetDateTime.now();

    @Column(name = "processed", nullable = false)
    private Boolean processed = Boolean.FALSE;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    public static SubscriptionUpdateEvent ref(Long id) {
        if (id == null) return null;
        SubscriptionUpdateEvent x = new SubscriptionUpdateEvent();
        x.setSubscriptionUpdateEventId(id);
        return x;
    }
}
