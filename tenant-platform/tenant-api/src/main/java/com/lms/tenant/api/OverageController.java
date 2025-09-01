package com.lms.tenant.api;

import com.lms.tenant.core.OverageService;
import com.lms.tenant.core.dto.OverageResponse;
import com.lms.tenant.core.dto.RecordOverageRequest;
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
        return ResponseEntity.ok(service.record(tenantId, subscriptionId, req));
    }
}
