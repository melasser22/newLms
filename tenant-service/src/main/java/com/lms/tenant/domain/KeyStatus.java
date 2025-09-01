package com.lms.tenant.domain;

/**
 * Represents the status of a tenant's integration key.
 */
public enum KeyStatus {
    /**
     * The key is active and can be used for authentication.
     */
    ACTIVE,

    /**
     * The key has been revoked and can no longer be used.
     */
    REVOKED
}
