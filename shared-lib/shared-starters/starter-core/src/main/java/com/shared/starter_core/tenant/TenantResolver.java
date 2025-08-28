package com.shared.starter_core.tenant;

import jakarta.servlet.http.HttpServletRequest;

public interface TenantResolver {
    /** Return tenant id if found; else null. Must NOT throw. */
    String resolve(HttpServletRequest request);
}