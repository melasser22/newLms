package com.ejada.starter_core.tenant;

import java.util.Objects;

/**
 * Represents the outcome of resolving a tenant identifier from the current
 * request.
 */
public record TenantResolution(Status status, String tenantId, String rawValue) {

    public enum Status {
        /** No tenant information was provided. */
        ABSENT,
        /** A valid tenant identifier was resolved. */
        PRESENT,
        /** A tenant value was provided but failed validation. */
        INVALID
    }

    public TenantResolution {
        status = Objects.requireNonNull(status, "status");
    }

    /** Convenience factory for absent results. */
    public static TenantResolution absent() {
        return new TenantResolution(Status.ABSENT, null, null);
    }

    /** Convenience factory for valid tenant identifiers. */
    public static TenantResolution present(String tenantId) {
        return new TenantResolution(Status.PRESENT, tenantId, tenantId);
    }

    /** Convenience factory for invalid tenant values. */
    public static TenantResolution invalid(String rawValue) {
        return new TenantResolution(Status.INVALID, null, rawValue);
    }

    /** @return {@code true} if a valid tenant identifier was resolved. */
    public boolean hasTenant() {
        return status == Status.PRESENT;
    }

    /** @return {@code true} if the resolver detected an invalid tenant value. */
    public boolean isInvalid() {
        return status == Status.INVALID;
    }

    /** @return {@code true} if no tenant information is available. */
    public boolean isAbsent() {
        return status == Status.ABSENT;
    }
}
