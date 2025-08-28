package com.common.enums;

/**
 * Centralized status enums for Shared services.
 * These provide consistency across modules when representing lifecycle states.
 */
public final class StatusEnums {

    private StatusEnums() {
        // prevent instantiation
    }

    /**
     * Generic entity lifecycle (used by most domain objects).
     */
    public enum EntityStatus {
        ACTIVE,
        INACTIVE,
        DELETED,
        ARCHIVED
    }

    /**
     * Approval workflow states (for requests, workflows, etc.).
     */
    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED
    }

    /**
     * Processing state (useful for async jobs, Kafka events, batch jobs).
     */
    public enum ProcessingStatus {
        QUEUED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        RETRYING
    }

    /**
     * Payment / transaction states (if applicable to Shared context).
     */
    public enum PaymentStatus {
        INITIATED,
        PENDING,
        SUCCESS,
        FAILED,
        REFUNDED
    }

    /**
     * API response status — maps to high-level outcomes.
     */
    public enum ApiStatus {
        SUCCESS,
        ERROR,
        WARNING
    }
}
