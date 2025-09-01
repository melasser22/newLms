package com.lms.tenantservice.web;

import com.lms.tenantservice.domain.Tenant;
import com.lms.tenantservice.domain.TenantStatus;
import com.lms.tenantservice.service.TenantLifecycleService;
import com.lms.tenantservice.web.dto.CreateTenantRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantLifecycleService tenantService;

    @PostMapping
    public ResponseEntity<Tenant> createTenant(@RequestBody @Valid CreateTenantRequest request) {
        Tenant created = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Tenant>> listTenants() {
        return ResponseEntity.ok(tenantService.listTenants());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.getTenant(id));
    }

    @PutMapping("/{id}/status/{status}")
    public ResponseEntity<Tenant> updateStatus(@PathVariable UUID id, @PathVariable TenantStatus status) {
        Tenant updated = tenantService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }
}
