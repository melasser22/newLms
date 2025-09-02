package com.ejada.billing.service;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ejada.billing.dto.OverageResponse;
import com.ejada.billing.dto.RecordOverageRequest;

import java.util.UUID;

/**
 * API contract for recording tenant overages.
 */
@RequestMapping("/tenants/{tenantId}/billing/overages")
public interface OverageService {

    /**
     * Record an overage for a tenant. If an idempotency key is supplied and a record exists,
     * the existing overage is returned.
     */
    @PostMapping
    OverageResponse record(
            @PathVariable("tenantId") UUID tenantId,
            @RequestParam(value = "subscriptionId", required = false) UUID subscriptionId,
            @RequestBody RecordOverageRequest request);
}

