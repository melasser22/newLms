package com.lms.tenant.controller;

import com.lms.tenant.controller.dto.EntitlementsResponse;
import com.lms.tenant.repository.EntitlementCacheRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/tenants/{tenantId}/entitlements")
public class EntitlementsController {

    private final EntitlementCacheRepository cacheRepository;

    public EntitlementsController(EntitlementCacheRepository cacheRepository) {
        this.cacheRepository = cacheRepository;
    }

    @GetMapping
    @Operation(summary = "Get entitlement snapshot timestamp")
    public EntitlementsResponse get(@PathVariable UUID tenantId) {
        return cacheRepository.findById(tenantId)
                .map(c -> new EntitlementsResponse(c.getSnapshot()))
                .orElseGet(() -> new EntitlementsResponse(Instant.EPOCH));
    }
}
