package com.lms.tenant.core.port;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Port to record overage events. */
public interface OveragePort {
    UUID recordOverage(UUID tenantId,
                       UUID subscriptionId,
                       String featureKey,
                       long quantity,
                       Long unitPriceMinor,
                       String currency,
                       Instant periodStart,
                       Instant periodEnd,
                       String idempotencyKey,
                       Map<String, Object> metadata);
}
