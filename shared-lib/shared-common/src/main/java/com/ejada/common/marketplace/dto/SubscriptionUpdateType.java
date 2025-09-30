package com.ejada.common.marketplace.dto;

/**
 * Update events emitted by the marketplace to drive subscription lifecycle transitions.
 */
public enum SubscriptionUpdateType {
    SUSPENDED,
    RESUMED,
    TERMINATED,
    EXPIRED
}
