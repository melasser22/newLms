package com.ejada.billing.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

import com.ejada.billing.enums.OverageStatus;

/**
 * JPA entity representing an overage recorded for a tenant.
 */
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

    public TenantOverage() {
    }

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public void setFeatureKey(String featureKey) {
        this.featureKey = featureKey;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public long getUnitPriceMinor() {
        return unitPriceMinor;
    }

    public void setUnitPriceMinor(long unitPriceMinor) {
        this.unitPriceMinor = unitPriceMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(Instant periodStart) {
        this.periodStart = periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(Instant periodEnd) {
        this.periodEnd = periodEnd;
    }

    public OverageStatus getStatus() {
        return status;
    }

    public void setStatus(OverageStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}

