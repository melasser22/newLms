package com.ejada.starter_core.tenant;

import java.util.regex.Pattern;

/**
 * Shared tenant identifier validation utilities.
 */
final class TenantIdValidator {

    private static final Pattern TENANT_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,36}");

    private TenantIdValidator() {
        // utility class
    }

    static TenantResolution validate(String candidate) {
        if (candidate == null) {
            return TenantResolution.absent();
        }
        String trimmed = candidate.trim();
        if (trimmed.isEmpty()) {
            return TenantResolution.absent();
        }
        if (!TENANT_PATTERN.matcher(trimmed).matches()) {
            return TenantResolution.invalid(trimmed);
        }
        return TenantResolution.present(trimmed);
    }
}
