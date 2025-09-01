package com.lms.tenant.controller;

import com.lms.tenant.controller.dto.ManualOverageRequest;
import com.lms.tenant.service.OverageService;
import com.lms.tenant.service.dto.OverageRecordDto;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tenants/{tenantId}/overages")
public class OverageController {

    private final OverageService overageService;

    public OverageController(OverageService overageService) {
        this.overageService = overageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record manual overage")
    public OverageRecordDto record(@PathVariable UUID tenantId, @RequestBody ManualOverageRequest request) {
        return overageService.record(tenantId, request.featureKey(), request.quantity(),
                request.unitPriceMinor(), request.currency(),
                request.periodStart(), request.periodEnd(), request.idemKey());
    }
}
