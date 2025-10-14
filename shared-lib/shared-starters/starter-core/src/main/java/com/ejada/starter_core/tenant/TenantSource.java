package com.ejada.starter_core.tenant;

/**
 * Origin of a resolved tenant identifier. Useful for logging/debugging and for
 * applying conditional policies when multiple resolution strategies are
 * enabled.
 */
public enum TenantSource {
    /** No tenant information available. */
    NONE,
    /** Tenant resolved from an authenticated JWT. */
    JWT,
    /** Tenant resolved from the {@code X-Tenant-Id} header. */
    HEADER,
    /** Tenant resolved from a query parameter. */
    QUERY_PARAM,
    /** Tenant resolved from the request subdomain. */
    SUBDOMAIN,
    /** Tenant resolved from an administrative path segment. */
    ADMIN_PATH
}

