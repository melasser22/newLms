package com.lms.billing.web;

import com.lms.billing.core.BillingService;
import com.shared.billing.api.OverageResponse;
import com.shared.billing.api.OverageService;
import com.shared.billing.api.RecordOverageRequest;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class OverageController implements OverageService {

    private final BillingService service;

    public OverageController(BillingService service) {
        this.service = service;
    }

    @Override
    public OverageResponse record(UUID tenantId, UUID subscriptionId, RecordOverageRequest request) {
        return service.record(tenantId, subscriptionId, request);
    }
}
