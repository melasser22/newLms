package com.ejada.subscription.model;

/**
 * Internal approval status for a subscription lifecycle. Values are persisted
 * as upper case strings to align with database representation.
 */
public enum SubscriptionApprovalStatus {
    PENDING_APPROVAL,
    AUTO_APPROVED,
    APPROVED,
    REJECTED;

    public static boolean isPending(final String status) {
        return PENDING_APPROVAL.name().equalsIgnoreCase(status);
    }

    public static boolean isApproved(final String status) {
        return APPROVED.name().equalsIgnoreCase(status)
                || AUTO_APPROVED.name().equalsIgnoreCase(status);
    }
}
