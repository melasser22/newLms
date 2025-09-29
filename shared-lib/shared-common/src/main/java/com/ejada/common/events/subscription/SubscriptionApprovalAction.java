package com.ejada.common.events.subscription;

/**
 * High-level actions for the subscription approval workflow.
 */
public enum SubscriptionApprovalAction {
    /**
     * A new subscription was created and requires approval by an administrator.
     */
    REQUEST,

    /**
     * The pending subscription approval was granted by an administrator.
     */
    APPROVED,

    /**
     * The pending subscription approval was rejected by an administrator.
     */
    REJECTED
}
