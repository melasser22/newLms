package com.lms.billing.web;

import com.lms.billing.core.BillingService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tenants/{tenantId}/overages")
@Validated
public class OverageController {

    private final BillingService service;

    public OverageController(BillingService service) {
        this.service = service;
    }

    @Override
    public OverageResponse record(UUID tenantId, UUID subscriptionId, RecordOverageRequest request) {
        return service.record(tenantId, subscriptionId, request);
    }
}
