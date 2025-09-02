package com.ejada.tenant.core.dto;

import java.time.Instant;
import java.util.UUID;

public record OverageResponse(
        UUID overageId,
        UUID tenantId,
        String featureKey,
        long quantity,
        long unitPriceMinor,
        String currency,
        Instant occurredAt,
        Instant periodStart,
        Instant periodEnd,
        String status) {
}
