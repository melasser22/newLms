package com.lms.billing.web;

import com.lms.billing.core.BillingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/tenants/{tenantId}/overages")
public class OverageController {

    private final BillingService service;

    public OverageController(BillingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UUID> record(@PathVariable UUID tenantId,
                                       @RequestParam(required = false) UUID subscriptionId,
                                       @RequestBody Map<String, Object> body) {
        UUID id = service.record(
                tenantId,
                subscriptionId,
                (String) body.get("featureKey"),
                ((Number) body.get("quantity")).longValue(),
                body.get("unitPriceMinor") == null ? null : ((Number) body.get("unitPriceMinor")).longValue(),
                (String) body.getOrDefault("currency", "USD"),
                Instant.parse((String) body.get("periodStart")),
                Instant.parse((String) body.get("periodEnd")),
                (String) body.get("idempotencyKey"));
        return ResponseEntity.ok(id);
    }
}
