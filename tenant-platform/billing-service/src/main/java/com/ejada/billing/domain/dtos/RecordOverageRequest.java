package com.ejada.billing.domain.dtos;

import java.time.Instant;
import java.util.Map;

/**
 * Request payload for recording an overage.
 */
public record RecordOverageRequest(
        String featureKey,
        long quantity,
        Long unitPriceMinor,
        String currency,
        Instant occurredAt,
        Instant periodStart,
        Instant periodEnd,
        String idempotencyKey,
        Map<String, Object> metadata
) {
}

