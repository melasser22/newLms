package com.lms.tenant.core.dto;

import java.time.Instant;
import java.util.Map;

/** Request to record an overage. */
public record RecordOverageRequest(String featureKey,
                                   long quantity,
                                   Long unitPriceMinor,
                                   String currency,
                                   Instant periodStart,
                                   Instant periodEnd,
                                   String idempotencyKey,
                                   Map<String, Object> metadata) {
}
