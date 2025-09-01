package com.lms.billing.core;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class BillingService {

    private final OveragePort port;

    public BillingService(OveragePort port) {
        this.port = port;
    }

    public UUID record(UUID tenantId, UUID subscriptionId, String feature, long qty,
                       Long price, String currency, Instant start, Instant end, String idemKey) {
        return port.recordOverage(tenantId, subscriptionId, feature, qty, price, currency, start, end, idemKey);
    }
}
