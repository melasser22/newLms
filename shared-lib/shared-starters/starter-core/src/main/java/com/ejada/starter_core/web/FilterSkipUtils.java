package com.ejada.starter_core.web;

import java.util.Arrays;
import java.util.Objects;

import org.springframework.util.AntPathMatcher;

/**
 * Utility methods for handling request filter skip patterns.
 */
public final class FilterSkipUtils {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static final String[] DEFAULT_SKIP_PATTERNS = new String[] {
            "/actuator/**",
            "/health/**",
            "/error",
            "/error/**",
            "/v3/api-docs/**",
            "/swagger/**",
            "/swagger-ui/**",
            "/docs/**",
            "/static/**",
            "/webjars/**",
            "/favicon.ico"
    };

    private FilterSkipUtils() {
        // utility class
    }

    /**
     * @return a defensive copy of the default skip patterns.
     */
    public static String[] defaultPatterns() {
        return DEFAULT_SKIP_PATTERNS.clone();
    }

    /**
     * Normalize the provided patterns by removing null entries and falling back
     * to the defaults when necessary.
     */
    public static String[] copyOrDefault(String[] patterns) {
        if (patterns == null || patterns.length == 0) {
            return defaultPatterns();
        }
        return Arrays.stream(patterns)
                .filter(Objects::nonNull)
                .toArray(String[]::new);
    }

    /**
     * Determine whether the given request path matches any of the provided skip
     * patterns.
     */
    public static boolean shouldSkip(String path, String[] patterns) {
        if (path == null || patterns == null || patterns.length == 0) {
            return false;
        }
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            if (containsWildcards(pattern)) {
                if (MATCHER.match(pattern, path)) {
                    return true;
                }
            } else if (path.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsWildcards(String pattern) {
        return pattern.indexOf('*') >= 0 || pattern.indexOf('?') >= 0;
    }
}
