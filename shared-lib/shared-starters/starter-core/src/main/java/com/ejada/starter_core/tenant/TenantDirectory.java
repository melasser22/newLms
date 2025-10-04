package com.ejada.starter_core.tenant;

import java.util.Optional;
import java.util.UUID;

/**
 * Pluggable directory used by {@link TenantResolver} implementations to look up
 * tenant metadata from the hosting application. Applications are expected to
 * provide a concrete implementation backed by their persistence layer or an
 * external directory service.
 */
public interface TenantDirectory {

    /**
     * Resolve tenant information by its canonical UUID identifier.
     *
     * @param tenantId security tenant identifier
     * @return optional tenant metadata
     */
    Optional<TenantRecord> findById(UUID tenantId);

    /**
     * Resolve tenant information by its human readable slug/code.
     *
     * @param slug tenant slug (case insensitive)
     * @return optional tenant metadata
     */
    Optional<TenantRecord> findBySlug(String slug);

    /**
     * Resolve tenant information using a public subdomain (case insensitive).
     * Implementations may simply delegate to {@link #findBySlug(String)} if the
     * subdomain and slug are equivalent in the data model.
     *
     * @param subdomain public subdomain
     * @return optional tenant metadata
     */
    default Optional<TenantRecord> findBySubdomain(String subdomain) {
        return findBySlug(subdomain);
    }

    /**
     * Represents a lightweight view of a tenant used purely for request
     * scoping. Implementations should avoid exposing sensitive data here.
     *
     * @param id canonical UUID identifier used for security scoping
     * @param slug optional human readable slug/code (lower case)
     * @param active whether the tenant is currently active
     */
    record TenantRecord(UUID id, String slug, boolean active) {
        public TenantRecord {
            if (id == null) {
                throw new IllegalArgumentException("id is required");
            }
        }

        public boolean isInactive() {
            return !active;
        }
    }

    /**
     * A no-op directory that never resolves tenants. Useful for tests or
     * disabling multi-tenancy explicitly.
     */
    static TenantDirectory noop() {
        return new TenantDirectory() {
            @Override
            public Optional<TenantRecord> findById(UUID tenantId) {
                return Optional.empty();
            }

            @Override
            public Optional<TenantRecord> findBySlug(String slug) {
                return Optional.empty();
            }
        };
    }
}

