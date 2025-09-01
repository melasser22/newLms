package com.lms.tenant.api.dto;

import com.lms.tenant.domain.TenantStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for representing a tenant in API responses.
 */
public record TenantResponse(
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
