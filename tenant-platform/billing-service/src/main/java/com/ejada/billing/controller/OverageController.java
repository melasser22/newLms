package com.ejada.billing.controller;

import com.ejada.billing.dto.OverageResponse;
import com.ejada.tenant.core.dto.RecordOverageRequest;
import com.ejada.billing.service.BillingService;
import com.ejada.billing.service.OverageService;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
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
