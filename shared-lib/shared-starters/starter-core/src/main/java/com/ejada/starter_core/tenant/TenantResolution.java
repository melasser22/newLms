package com.ejada.starter_core.tenant;

import java.util.Objects;

/**
 * Represents the outcome of resolving a tenant identifier from the current
 * request.
 */
public record TenantResolution(Status status,
                               String tenantId,
                               String rawValue,
                               TenantError error,
                               TenantSource source) {

    public enum Status {
        /** No tenant information was provided. */
        ABSENT,
        /** A valid tenant identifier was resolved. */
        PRESENT,
        /** A tenant value was provided but failed validation. */
        INVALID,
        /** Tenant information was provided but could not be accepted. */
        ERROR
    }

    public TenantResolution {
        status = Objects.requireNonNull(status, "status");
        source = source == null ? TenantSource.NONE : source;
    }

    /** Convenience factory for absent results. */
    public static TenantResolution absent() {
        return new TenantResolution(Status.ABSENT, null, null, null, TenantSource.NONE);
    }

    /** Convenience factory for valid tenant identifiers. */
    public static TenantResolution present(String tenantId, TenantSource source) {
        return new TenantResolution(Status.PRESENT, tenantId, tenantId, null, source);
    }

    /** Convenience factory for invalid tenant values. */
    public static TenantResolution invalid(String rawValue, TenantSource source) {
        return new TenantResolution(Status.INVALID, null, rawValue, TenantError.badRequest("TENANT_INVALID", "Invalid tenant identifier"), source);
    }

    /** Convenience factory for an error outcome. */
    public static TenantResolution error(TenantError error, String rawValue, TenantSource source) {
        return new TenantResolution(Status.ERROR, null, rawValue, error, source);
    }

    /** @return {@code true} if a valid tenant identifier was resolved. */
    public boolean hasTenant() {
        return status == Status.PRESENT;
    }

    /** @return {@code true} if the resolver detected an invalid tenant value. */
    public boolean isInvalid() {
        return status == Status.INVALID;
    }

    /** @return {@code true} if the resolver returned any kind of error. */
    public boolean hasError() {
        return status == Status.ERROR || status == Status.INVALID;
    }

    /** @return {@code true} if no tenant information is available. */
    public boolean isAbsent() {
        return status == Status.ABSENT;
    }
}
