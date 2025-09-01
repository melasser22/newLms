package com.lms.tenant.core;

import java.time.Instant;
import java.util.UUID;

public interface OveragePort {
    UUID recordOverage(UUID tenantId,
                       UUID subscriptionId,
                       String featureKey,
                       long quantity,
                       long unitPriceMinor,
                       String currency,
                       Instant periodStart,
                       Instant periodEnd,
                       String idempotencyKey);
}
