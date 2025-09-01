package com.lms.entitlement.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "subscription_item")
public class SubscriptionItem {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subscription_id")
    private TenantSubscription subscription;

    private String description;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TenantSubscription getSubscription() { return subscription; }
    public void setSubscription(TenantSubscription subscription) { this.subscription = subscription; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
