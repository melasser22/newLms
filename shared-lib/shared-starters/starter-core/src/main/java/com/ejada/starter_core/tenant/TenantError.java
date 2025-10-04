package com.ejada.starter_core.tenant;

import java.util.Objects;

/**
 * Error descriptor returned as part of {@link TenantResolution} when the
 * resolver determines that the incoming request cannot be associated with a
 * tenant.
 */
public record TenantError(int httpStatus, String code, String message) {

    public TenantError {
        if (httpStatus < 100 || httpStatus > 599) {
            throw new IllegalArgumentException("Invalid HTTP status: " + httpStatus);
        }
        code = Objects.requireNonNullElse(code, "TENANT_ERROR");
        message = Objects.requireNonNullElse(message, "Tenant resolution failed");
    }

    public static TenantError badRequest(String code, String message) {
        return new TenantError(400, code, message);
    }

    public static TenantError unauthorized(String code, String message) {
        return new TenantError(401, code, message);
    }

    public static TenantError forbidden(String code, String message) {
        return new TenantError(403, code, message);
    }

    public static TenantError notFound(String code, String message) {
        return new TenantError(404, code, message);
    }
}

