package com.ejada.tenant.service.web;

import com.ejada.tenant.persistence.entity.Tenant;
import com.ejada.tenant.service.TenantService;
import com.ejada.tenant.service.dto.CreateTenantRequest;
import com.ejada.tenant.service.dto.TenantResponse;
import com.ejada.tenant.service.dto.ToggleOverageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<TenantResponse> create(@RequestBody CreateTenantRequest request) {
        Tenant tenant = tenantService.createTenant(request.slug(), request.name());
        TenantResponse response = new TenantResponse(tenant.getId(), tenant.getSlug(), tenant.getName(), tenant.isOverageEnabled());
        return ResponseEntity.created(URI.create("/api/tenants/" + tenant.getId())).body(response);
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

    @PatchMapping("/{tenantId}/overage")
    public ResponseEntity<Void> toggleOverage(@PathVariable UUID tenantId, @RequestBody ToggleOverageRequest request) {
        tenantService.setOverage(tenantId, request.enabled());
        return ResponseEntity.accepted().build();
    }
}
