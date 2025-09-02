package com.ejada.tenant.api;

import com.ejada.tenant.core.OverageService;
import com.ejada.tenant.core.dto.OverageResponse;
import com.ejada.tenant.core.dto.RecordOverageRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tenants/{tenantId}/overages")
@Validated
public class OverageController {
    private final OverageService service;

    public OverageController(OverageService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OverageResponse> record(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) UUID subscriptionId,
            @Valid @RequestBody RecordOverageRequest req) {
        long unitPrice = req.unitPriceMinor() == null ? 0L : req.unitPriceMinor();
        var id = service.record(tenantId, subscriptionId, req.featureKey(),
                req.quantity(), unitPrice, req.currency(),
                req.periodStart(), req.periodEnd(), req.idempotencyKey());
        var res = new OverageResponse(id, tenantId, req.featureKey(), req.quantity(),
                unitPrice, req.currency(), req.occurredAt(),
                req.periodStart(), req.periodEnd(), "RECORDED");
        return ResponseEntity.ok(res);
    }
}
