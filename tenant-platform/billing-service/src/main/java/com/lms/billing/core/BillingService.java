package com.lms.billing.core;

import com.shared.billing.api.OverageResponse;
import com.shared.billing.api.RecordOverageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BillingService {

    private final OveragePort port;

    public BillingService(OveragePort port) {
        this.port = port;
    }

    public OverageResponse record(UUID tenantId, UUID subscriptionId, RecordOverageRequest request) {
        return port.recordOverage(tenantId, subscriptionId, request);
    }
}
