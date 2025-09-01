package com.lms.tenant.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents a tenant's integration API key.
 *
 * @param id The unique identifier for the key.
 * @param tenantId The ID of the tenant who owns the key.
 * @param keyPrefix A safe, non-secret prefix for identifying the key.
 * @param keyHash The securely hashed version of the key.
 * @param name A user-friendly name for the key.
 * @param scopes The permissions associated with the key.
 * @param rateLimitPerMin The rate limit for the key, in requests per minute.
 * @param createdAt The timestamp when the key was created.
 * @param lastUsedAt The timestamp when the key was last used.
 * @param expiresAt The timestamp when the key expires.
 * @param status The current status of the key.
 */
public record IntegrationKey(
    UUID id,
    UUID tenantId,
    String keyPrefix,
    byte[] keyHash,
    String name,
    List<String> scopes,
    Integer rateLimitPerMin,
    Instant createdAt,
    Instant lastUsedAt,
    Instant expiresAt,
    KeyStatus status
) {
}
