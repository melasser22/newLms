package com.lms.tenant.domain;

/**
 * Represents the lifecycle status of a tenant.
 */
public enum TenantStatus {
    /**
     * The tenant is active and can use the system.
     */
    ACTIVE,

    /**
     * The tenant is temporarily suspended and cannot access the system.
     */
    SUSPENDED,

    /**
     * The tenant has been permanently archived, typically after being deleted.
     */
    ARCHIVED
}
