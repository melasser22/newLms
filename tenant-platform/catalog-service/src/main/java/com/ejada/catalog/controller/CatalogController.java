package com.ejada.catalog.controller;

import com.ejada.catalog.service.CatalogService;
import com.ejada.catalog.service.FeaturePolicyPort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/catalog")
@Validated
public class CatalogController {

    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @GetMapping("/effective")
    public ResponseEntity<FeaturePolicyPort.EffectiveFeature> effective(
            @RequestParam("tierId") String tierId,
            @RequestParam("tenantId") UUID tenantId,
            @RequestParam("featureKey") String featureKey) {
        return ResponseEntity.ok(service.effective(tierId, tenantId, featureKey));
    }
}
