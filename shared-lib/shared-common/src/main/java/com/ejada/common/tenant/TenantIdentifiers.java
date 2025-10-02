package com.ejada.common.tenant;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.springframework.util.StringUtils;

/**
 * Utility helpers for deriving stable identifiers from tenant metadata.
 */
public final class TenantIdentifiers {

    private static final String NAMESPACE_PREFIX = "tenant:";

    private TenantIdentifiers() {
        // utility class
    }

    /**
     * Deterministically derives a UUID that can be used as the security tenant identifier
     * from the supplied tenant code. The same tenant code will always yield the same UUID,
     * allowing independent services to agree on the identifier without an extra lookup.
     *
     * @param tenantCode canonical tenant code (case insensitive)
     * @return derived UUID
     * @throws IllegalArgumentException if {@code tenantCode} is blank
     */
    public static UUID deriveTenantId(final String tenantCode) {
        if (!StringUtils.hasText(tenantCode)) {
            throw new IllegalArgumentException("tenantCode must not be blank");
        }

        String normalized = tenantCode.trim().toLowerCase(Locale.ROOT);
        byte[] bytes = (NAMESPACE_PREFIX + normalized).getBytes(StandardCharsets.UTF_8);
        return UUID.nameUUIDFromBytes(bytes);
    }
}
