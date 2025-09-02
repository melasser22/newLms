package com.ejada.billing.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response payload returned after recording an overage.
 */
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
        String status
) {
}

