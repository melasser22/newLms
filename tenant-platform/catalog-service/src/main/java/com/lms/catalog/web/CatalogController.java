package com.lms.catalog.web;

import com.lms.catalog.core.CatalogService;
import com.lms.catalog.core.FeaturePolicyPort;
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
    public ResponseEntity<FeaturePolicyPort.EffectiveFeature> effective(@RequestParam String tierId,
                                                                        @RequestParam UUID tenantId,
                                                                        @RequestParam String featureKey) {
        return ResponseEntity.ok(service.effective(tierId, tenantId, featureKey));
    }
}
