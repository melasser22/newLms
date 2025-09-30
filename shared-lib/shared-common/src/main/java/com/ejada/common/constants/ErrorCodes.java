package com.ejada.common.constants;

/**
 * Centralized error codes for Shared services.
 * Each code should be unique and stable, so clients can rely on it.
 * 
 * Format recommendation:
 * ERR-{DOMAIN}-{DETAIL}
 * Example: ERR-AUTH-INVALID_TOKEN
 */
public final class ErrorCodes {

    private ErrorCodes() {
        // prevent instantiation
    }

    // 🔐 Authentication & Authorization
    public static final String AUTH_INVALID_TOKEN = "ERR-AUTH-INVALID_TOKEN";
    public static final String AUTH_EXPIRED_TOKEN = "ERR-AUTH-EXPIRED_TOKEN";
    public static final String AUTH_UNAUTHORIZED = "ERR-AUTH-UNAUTHORIZED";
    public static final String AUTH_FORBIDDEN = "ERR-AUTH-FORBIDDEN";
    public static final String AUTH_MISSING_CREDENTIALS = "ERR-AUTH-MISSING_CREDENTIALS";
    public static final String AUTH_INVALID_CREDENTIALS = "ERR-AUTH-INVALID_CREDENTIALS";
    public static final String AUTH_HISTORY_UNAVAILABLE = "ERR-AUTH-HISTORY_UNAVAILABLE";
    public static final String AUTH_DATA_ACCESS = "ERR-AUTH-DATA_ACCESS";

    // 🏢 Multi-tenancy
    public static final String TENANT_NOT_FOUND = "ERR-TENANT-NOT_FOUND";
    public static final String TENANT_DISABLED = "ERR-TENANT-DISABLED";
    public static final String TENANT_ACCESS_DENIED = "ERR-TENANT-ACCESS_DENIED";

    // 📦 Data & Validation
    public static final String VALIDATION_ERROR = "ERR-VALIDATION";
    public static final String DATA_NOT_FOUND = "ERR-DATA-NOT_FOUND";
    public static final String DATA_DUPLICATE = "ERR-DATA-DUPLICATE";
    public static final String DATA_INTEGRITY = "ERR-DATA-INTEGRITY";

    // ⚙️ System & Infrastructure
    public static final String INTERNAL_ERROR = "ERR-SYSTEM-INTERNAL";
    public static final String SERVICE_UNAVAILABLE = "ERR-SYSTEM-UNAVAILABLE";
    public static final String TIMEOUT = "ERR-SYSTEM-TIMEOUT";
    public static final String DEPENDENCY_FAILURE = "ERR-SYSTEM-DEPENDENCY_FAILURE";

    // 📡 API & Requests
    public static final String API_BAD_REQUEST = "ERR-API-BAD_REQUEST";
    public static final String API_UNSUPPORTED_MEDIA = "ERR-API-UNSUPPORTED_MEDIA";
    public static final String API_RATE_LIMIT_EXCEEDED = "ERR-API-RATE_LIMIT";
    public static final String API_UNPROCESSABLE_ENTITY = "ERR-API-UNPROCESSABLE_ENTITY";

    // 💳 Payment / Billing (optional if relevant)
    public static final String PAYMENT_FAILED = "ERR-PAYMENT-FAILED";
    public static final String PAYMENT_DECLINED = "ERR-PAYMENT-DECLINED";
    public static final String PAYMENT_TIMEOUT = "ERR-PAYMENT-TIMEOUT";

    // Business Rules

    public static final String BUSINESS_RULE_VIOLATION = "ERR-BUSINESS-RULE";

    // General

    public static final String NOT_FOUND = "ERR-NOT-FOUND";

}
