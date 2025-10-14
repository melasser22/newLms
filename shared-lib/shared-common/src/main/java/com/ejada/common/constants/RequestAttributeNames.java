package com.ejada.common.constants;

/**
 * Canonical request attribute names that Shared components use to communicate
 * contextual information (tenant id, correlation id, etc.) across servlet
 * filters and framework integrations. Using constants avoids subtle bugs caused
 * by typos and makes it easier for downstream modules to participate in the
 * propagation pipeline.
 */
public final class RequestAttributeNames {

    private RequestAttributeNames() {
        // utility class
    }

    /** Attribute storing the current tenant identifier (String). */
    public static final String TENANT_ID = "__shared_tenant_id";

    /** Attribute storing the current correlation identifier (String). */
    public static final String CORRELATION_ID = "__shared_correlation_id";
}

