package com.ejada.common.constants;

/**
 * Centralized header names used across Shared services.
 * Use these constants instead of hard-coding strings in controllers/filters.
 */
public final class HeaderNames {

    private HeaderNames() {
        // prevent instantiation
    }

    // 🔐 Authentication & Security
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer";
    public static final String REFRESH_TOKEN = "X-Refresh-Token";

    // 🏢 Multi-tenancy
    /**
     * Canonical tenant identifier header. All services should read/write this
     * header only and avoid using any legacy variants.
     */
    public static final String X_TENANT_ID = "X-Tenant-Id";
    /**
     * Legacy tenant header variant kept for backwards compatibility with
     * services that still emit/expect the underscore format.
     */
    public static final String X_TENANT_ID_LEGACY = "x_tenant_id";
    public static final String TENANT_KEY = "X-Tenant-Key";
    public static final String MESSAGE_ID = "x-msg-id";

    // 📦 Request Context / Tracing
    public static final String REQUEST_ID = "X-Request-Id";
    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String SPAN_ID = "X-Span-Id";

    // 🌐 Client / Device Metadata
    public static final String USER_AGENT = "User-Agent";
    public static final String CLIENT_IP = "X-Forwarded-For";
    public static final String FORWARDED_PROTO = "X-Forwarded-Proto";
    public static final String CLIENT_APP = "X-Client-App";
    public static final String CLIENT_VERSION = "X-Client-Version";
    public static final String DEVICE_ID = "X-Device-Id";
    public static final String PLATFORM = "X-Platform"; // e.g. iOS, Android, Web
    public static final String USER_ID = "X-USER_ID"; // e.g. iOS, Android, Web

    // 📜 Content / Localization
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String ACCEPT = "Accept";
    public static final String ACCEPT_LANGUAGE = "Accept-Language";
    public static final String LOCALE = "X-Locale";

    // ⏱️ Performance / Caching
    public static final String CACHE_CONTROL = "Cache-Control";
    public static final String IF_NONE_MATCH = "If-None-Match";
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

    // 🛡️ Custom Security Headers
    public static final String CSRF_TOKEN = "X-CSRF-Token";
    public static final String API_KEY = "X-API-Key";
}
