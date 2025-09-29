package com.ejada.starter_core.tenant;

import jakarta.servlet.http.HttpServletRequest;

public interface TenantResolver {
    /** Resolve the tenant information for the given request. */
    TenantResolution resolve(HttpServletRequest request);

    /** Resolver that always reports the tenant as absent. */
    static TenantResolver noop() {
        return request -> TenantResolution.absent();
    }
}