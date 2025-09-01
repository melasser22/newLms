package com.lms.tenant.core.service;

import com.lms.tenant.core.dto.OverageResponse;
import com.lms.tenant.core.dto.RecordOverageRequest;
import com.lms.tenant.core.port.OveragePort;

import java.util.UUID;

/** Service facade over {@link OveragePort}. */
public class OverageService {

    private final OveragePort overagePort;

    public OverageService(OveragePort overagePort) {
        this.overagePort = overagePort;
    }

    public OverageResponse record(UUID tenantId, UUID subscriptionId, RecordOverageRequest request) {
        UUID id = overagePort.recordOverage(tenantId, subscriptionId, request.featureKey(), request.quantity(),
                request.unitPriceMinor(), request.currency(), request.periodStart(), request.periodEnd(),
                request.idempotencyKey(), request.metadata());
        return new OverageResponse(id, tenantId, request.featureKey(), request.quantity(), request.unitPriceMinor(),
                request.currency(), request.periodStart(), request.periodEnd(), "RECORDED");
    }
}
