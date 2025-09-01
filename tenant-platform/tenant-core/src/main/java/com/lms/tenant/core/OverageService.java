package com.lms.tenant.core;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class OverageService {
    private final OveragePort overagePort;

    public OverageService(OveragePort overagePort) {
        this.overagePort = overagePort;
    }

    public UUID record(UUID tenantId,
                       UUID subscriptionId,
                       String featureKey,
                       long quantity,
                       long unitPriceMinor,
                       String currency,
                       Instant periodStart,
                       Instant periodEnd,
                       String idempotencyKey) {
        return overagePort.recordOverage(tenantId, subscriptionId, featureKey, quantity,
                unitPriceMinor, currency, periodStart, periodEnd, idempotencyKey);
    }
}
