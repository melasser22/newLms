package com.ejada.starter_core.config;

import java.util.Set;

/**
 * Central place for default skip-pattern configuration used by multiple filters.
 */
public final class SkipPatternDefaults {

    private SkipPatternDefaults() {
    }

    private static final String[] CORRELATION_AND_TENANT_SKIP_PATTERNS = new String[] {
        "/actuator/**",
        "/swagger-ui/**",
        "/**/api/*/swagger-ui/**",
        "/**/api/*/swagger-ui.html",
        "/v3/api-docs/**",
        "/**/api/*/v3/api-docs/**",
        "/api/v1/admin/**",
        "/static/**",
        "/webjars/**",
        "/error",
        "/favicon.ico"
    };

    private static final Set<String> CONTEXT_SKIP_PREFIXES = Set.of(
        "/actuator",
        "/health",
        "/error",
        "/v3/api-docs",
        "/swagger",
        "/swagger-ui",
        "/docs"
    );

    /**
     * Default Ant-style skip patterns shared by correlation and tenant filters.
     *
     * @return a defensive copy of the defaults to avoid accidental mutation
     */
    public static String[] correlationAndTenantSkipPatterns() {
        return CORRELATION_AND_TENANT_SKIP_PATTERNS.clone();
    }

    /**
     * Default path prefixes that should not go through the context filter.
     */
    public static Set<String> contextSkipPrefixes() {
        return CONTEXT_SKIP_PREFIXES;
    }
}

