package com.ejada.billing.application.services;


import org.springframework.stereotype.Service;

import com.ejada.billing.application.ports.OveragePort;
import com.ejada.billing.domain.dtos.OverageResponse;
import com.ejada.billing.domain.dtos.RecordOverageRequest;

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
