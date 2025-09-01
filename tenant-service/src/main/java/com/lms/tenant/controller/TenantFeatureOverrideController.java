package com.lms.tenant.controller;

import com.lms.tenant.controller.dto.OverageOverrideRequest;
import com.lms.tenant.entity.Feature;
import com.lms.tenant.entity.Tenant;
import com.lms.tenant.entity.TenantFeatureOverride;
import com.lms.tenant.repository.FeatureRepository;
import com.lms.tenant.repository.TenantFeatureOverrideRepository;
import com.lms.tenant.repository.TenantRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tenants/{tenantId}/features/{featureKey}/overage-override")
public class TenantFeatureOverrideController {

    private final TenantRepository tenantRepository;
    private final FeatureRepository featureRepository;
    private final TenantFeatureOverrideRepository overrideRepository;

    public TenantFeatureOverrideController(TenantRepository tenantRepository,
                                           FeatureRepository featureRepository,
                                           TenantFeatureOverrideRepository overrideRepository) {
        this.tenantRepository = tenantRepository;
        this.featureRepository = featureRepository;
        this.overrideRepository = overrideRepository;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Upsert tenant feature overage override")
    public void upsert(@PathVariable UUID tenantId, @PathVariable String featureKey,
                       @RequestBody OverageOverrideRequest req) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        Feature feature = featureRepository.findById(featureKey).orElseThrow();
        TenantFeatureOverride override = overrideRepository.findByTenantAndFeature(tenant, feature)
                .orElseGet(() -> {
                    TenantFeatureOverride tfo = new TenantFeatureOverride();
                    tfo.setTenant(tenant);
                    tfo.setFeature(feature);
                    return tfo;
                });
        override.setAllowOverage(req.allowOverage());
        override.setOverageUnitPriceMinor(req.overageUnitPriceMinor());
        override.setOverageCurrency(req.overageCurrency());
        overrideRepository.save(override);
    }
}
