package com.ejada.catalog.controller;

import com.ejada.catalog.core.CatalogService;
import com.ejada.catalog.core.FeaturePolicyPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/tenants/{tenantId}/features")
public class TenantFeatureOverrideController {

    private final CatalogService service;

    public TenantFeatureOverrideController(CatalogService service) {
        this.service = service;
    }

    @PutMapping("/{featureKey}/override")
    public ResponseEntity<Void> override(@PathVariable UUID tenantId,
                                         @PathVariable String featureKey,
                                         @RequestBody FeaturePolicyPort.FeatureOverride override) {
        service.upsertOverride(tenantId, featureKey, override);
        return ResponseEntity.noContent().build();
    }
}
