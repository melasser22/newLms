package com.lms.tenant.web;

import com.lms.tenant.core.TenantService;
import com.lms.tenant.core.TenantService.Tenant;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/tenants")
public class TenantController {

    private final TenantService service;

    public TenantController(TenantService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a tenant")
    public ResponseEntity<TenantResponse> create(@RequestBody CreateTenantRequest request) {
        Tenant tenant = service.createTenant(request.slug(), request.name());
        return ResponseEntity.created(URI.create("/tenants/" + tenant.id()))
                .body(new TenantResponse(tenant.id(), tenant.slug(), tenant.name(), tenant.overageEnabled()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read tenant")
    public ResponseEntity<TenantResponse> read(@PathVariable UUID id) {
        Tenant tenant = service.findTenant(id);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new TenantResponse(tenant.id(), tenant.slug(), tenant.name(), tenant.overageEnabled()));
    }

    @PutMapping("/{id}/overage-enabled")
    @Operation(summary = "Toggle overage flag")
    public ResponseEntity<Void> toggleOverage(@PathVariable UUID id, @RequestBody ToggleOverageRequest body) {
        service.toggleOverage(id, body.enabled());
        return ResponseEntity.accepted().build();
    }
}
