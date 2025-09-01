package com.lms.tenant.core.dto;

import java.time.Instant;
import java.util.UUID;

/** Placeholder DTO for caching entitlements. */
public record Entitlements(UUID tenantId, Instant computedAt) {
}
