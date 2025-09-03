package com.ejada.billing.service;


import org.springframework.stereotype.Service;

import com.ejada.billing.dto.OverageResponse;
import com.ejada.tenant.core.dto.RecordOverageRequest;

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
