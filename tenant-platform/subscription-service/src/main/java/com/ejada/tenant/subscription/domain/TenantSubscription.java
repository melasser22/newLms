package com.ejada.tenant.subscription.domain;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "tenant_subscription")
public class TenantSubscription {

    @Id
    @Column(name = "subscription_id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SubscriptionStatus status;

    @Column(name = "active")
    private boolean active;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubscriptionItem> items = new ArrayList<>();

    public TenantSubscription() {}

    public TenantSubscription(UUID id, UUID tenantId, SubscriptionStatus status, boolean active) {
        this.id = id;
        this.tenantId = tenantId;
        this.status = status;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<SubscriptionItem> getItems() {
        return items;
    }
}
