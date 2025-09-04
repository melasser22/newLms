package com.ejada.catalog.service;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import io.github.resilience4j.retry.annotation.Retry;

@Service
@Validated
public class CatalogService {

    private final FeaturePolicyPort port;

    public CatalogService(FeaturePolicyPort port) {
        this.port = port;
    }

    @Retry(name = "catalog-service")
    public FeaturePolicyPort.EffectiveFeature effective(String tierId, UUID tenantId, String featureKey) {
        return port.effective(tierId, tenantId, featureKey);
    }

    @Retry(name = "catalog-service")
    public void upsertOverride(UUID tenantId, String featureKey, @Valid FeaturePolicyPort.FeatureOverride override) {
        port.upsertOverride(tenantId, featureKey, override);
    }
}
