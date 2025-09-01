package com.lms.tenant.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents the core Tenant domain object.
 *
 * @param id The unique identifier for the tenant.
 * @param slug The unique, URL-friendly identifier for the tenant.
 * @param name The display name of the tenant.
 * @param status The current status of the tenant.
 * @param tierId The identifier of the subscription tier the tenant belongs to.
 * @param timezone The tenant's preferred timezone.
 * @param locale The tenant's preferred locale.
 * @param domains A list of custom domains associated with the tenant.
 * @param overageEnabled A flag indicating if the tenant can exceed their usage limits.
 * @param createdAt The timestamp when the tenant was created.
 * @param updatedAt The timestamp when the tenant was last updated.
 */
public record Tenant(
    UUID id,
    String slug,
    String name,
    TenantStatus status,
    String tierId,
    String timezone,
    String locale,
    List<String> domains,
    boolean overageEnabled,
    Instant createdAt,
    Instant updatedAt
) {
}
