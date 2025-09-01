package com.lms.tenant.service.web;

import com.lms.tenant.persistence.entity.Tenant;
import com.lms.tenant.service.TenantService;
import com.lms.tenant.service.dto.CreateTenantRequest;
import com.lms.tenant.service.dto.TenantResponse;
import com.lms.tenant.service.dto.ToggleOverageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/tenants")
public class TenantController {
    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<TenantResponse> create(@RequestBody CreateTenantRequest request) {
        Tenant tenant = tenantService.createTenant(request.slug(), request.name());
        TenantResponse response = new TenantResponse(tenant.getId(), tenant.getSlug(), tenant.getName(), tenant.isOverageEnabled());
        return ResponseEntity.created(URI.create("/tenants/" + tenant.getId())).body(response);
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> get(@PathVariable UUID tenantId) {
        Tenant tenant = tenantService.getTenant(tenantId);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        TenantResponse response = new TenantResponse(tenant.getId(), tenant.getSlug(), tenant.getName(), tenant.isOverageEnabled());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{tenantId}/overage-enabled")
    public ResponseEntity<Void> toggleOverage(@PathVariable UUID tenantId, @RequestBody ToggleOverageRequest request) {
        tenantService.setOverage(tenantId, request.enabled());
        return ResponseEntity.accepted().build();
    }
}
