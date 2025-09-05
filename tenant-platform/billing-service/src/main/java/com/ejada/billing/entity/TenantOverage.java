package com.ejada.billing.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

import com.ejada.billing.enums.OverageStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing an overage recorded for a tenant.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tenant_overage")
public class TenantOverage {

    @Id
    @Column(name = "overage_id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "feature_key", nullable = false)
    private String featureKey;

    @Column(name = "quantity", nullable = false)
    private long quantity;

    @Column(name = "unit_price_minor", nullable = false)
    private long unitPriceMinor;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OverageStatus status;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadataJson;
}

