package com.ejada.billing.web.controllers;

import com.ejada.billing.domain.dtos.OverageResponse;
import com.ejada.billing.domain.dtos.RecordOverageRequest;
import com.ejada.billing.application.services.BillingService;

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
