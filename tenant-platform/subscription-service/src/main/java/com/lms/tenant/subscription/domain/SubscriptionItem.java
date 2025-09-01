package com.lms.tenant.subscription.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "subscription_item")
public class SubscriptionItem {
    @Id
    @Column(name = "item_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private TenantSubscription subscription;

    @Column(name = "feature_key")
    private String featureKey;

    @Column(name = "quantity")
    private Long quantity;

    public SubscriptionItem() {}

    public SubscriptionItem(UUID id, TenantSubscription subscription, String featureKey, Long quantity) {
        this.id = id;
        this.subscription = subscription;
        this.featureKey = featureKey;
        this.quantity = quantity;
    }

    public UUID getId() {
        return id;
    }

    public TenantSubscription getSubscription() {
        return subscription;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public Long getQuantity() {
        return quantity;
    }
}
