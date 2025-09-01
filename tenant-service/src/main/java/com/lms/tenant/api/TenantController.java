package com.lms.tenant.api;

import com.lms.tenant.api.dto.CreateTenantRequest;
import com.lms.tenant.api.dto.TenantResponse;
import com.lms.tenant.api.mapper.TenantApiMapper;
import com.lms.tenant.application.port.in.TenantUseCase;
import com.lms.tenant.domain.Tenant;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tenants")
public class TenantController {

    private final TenantUseCase tenantUseCase;
    private final TenantApiMapper mapper;

    public TenantController(TenantUseCase tenantUseCase, TenantApiMapper mapper) {
        this.tenantUseCase = tenantUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        Tenant tenantToCreate = mapper.toDomain(request);
        Tenant createdTenant = tenantUseCase.createTenant(tenantToCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(createdTenant));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantResponse> getTenantById(@PathVariable UUID id) {
        return tenantUseCase.findTenantById(id)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<TenantResponse> getTenantBySlug(@PathVariable String slug) {
        return tenantUseCase.findTenantBySlug(slug)
            .map(mapper::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/overage-policy/enabled/{isEnabled}")
    public ResponseEntity<TenantResponse> setOveragePolicy(
        @PathVariable UUID id,
        @PathVariable boolean isEnabled) {
        try {
            Tenant updatedTenant = tenantUseCase.setOveragePolicy(id, isEnabled);
            return ResponseEntity.ok(mapper.toResponse(updatedTenant));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
