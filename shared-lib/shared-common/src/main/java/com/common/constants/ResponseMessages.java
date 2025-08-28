package com.common.constants;

/**
 * Standardized response messages for Shared APIs.
 * Use these instead of hard-coding strings in controllers/services.
 */
public final class ResponseMessages {

    private ResponseMessages() {
        // prevent instantiation
    }

    // ✅ Success
    public static final String SUCCESS = "Operation completed successfully";
    public static final String CREATED = "Resource created successfully";
    public static final String UPDATED = "Resource updated successfully";
    public static final String DELETED = "Resource deleted successfully";
    public static final String ACCEPTED = "Request accepted for processing";

    // ⚠️ Validation / Neutral
    public static final String INVALID_INPUT = "Invalid input data";
    public static final String MISSING_REQUIRED_FIELD = "Missing required field";
    public static final String DUPLICATE_ENTRY = "Duplicate entry detected";
    public static final String CONSTRAINT_VIOLATION = "Data constraint violation";

    // 🔍 Lookup / Retrieval
    public static final String NOT_FOUND = "Requested resource not found";
    public static final String FOUND = "Resource retrieved successfully";
    public static final String NO_CONTENT = "No content available";

    // 🔒 Security / Auth
    public static final String UNAUTHORIZED = "Unauthorized access";
    public static final String FORBIDDEN = "Forbidden action";
    public static final String TOKEN_EXPIRED = "Authentication token expired";
    public static final String TOKEN_INVALID = "Invalid authentication token";

    // ⚙️ System / Technical
    public static final String INTERNAL_ERROR = "Internal server error";
    public static final String SERVICE_UNAVAILABLE = "Service temporarily unavailable";
    public static final String TIMEOUT = "Request timed out";
    public static final String TOO_MANY_REQUESTS = "Too many requests — rate limit exceeded";

    // 👥 Tenant / Multi-tenancy
    public static final String TENANT_NOT_FOUND = "Tenant not found";
    public static final String TENANT_SUSPENDED = "Tenant suspended";
    public static final String TENANT_ACTIVE = "Tenant is active";

    // 📦 Generic operations
    public static final String OPERATION_PENDING = "Operation is pending";
    public static final String OPERATION_IN_PROGRESS = "Operation is in progress";
    public static final String OPERATION_FAILED = "Operation failed";

}
