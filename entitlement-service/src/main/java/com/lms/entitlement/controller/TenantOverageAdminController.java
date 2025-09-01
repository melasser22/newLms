package com.lms.entitlement.controller;

import com.lms.entitlement.repository.TenantRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tenants/{tenantId}/overage-policy")
public class TenantOverageAdminController {

    private final TenantRepository tenantRepository;

    public TenantOverageAdminController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @PutMapping("/enabled/{flag}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Toggle overage enabled flag for tenant")
    public void toggle(@PathVariable UUID tenantId, @PathVariable boolean flag) {
        tenantRepository.findById(tenantId).ifPresent(t -> {
            t.setOverageEnabled(flag);
            tenantRepository.save(t);
        });
    }
}
