package com.lms.tenant.core.dto;

import java.time.Instant;
import java.util.UUID;

/** Response after recording an overage. */
public record OverageResponse(UUID overageId,
                              UUID tenantId,
                              String featureKey,
                              long quantity,
                              Long unitPriceMinor,
                              String currency,
                              Instant periodStart,
                              Instant periodEnd,
                              String status) {
}
