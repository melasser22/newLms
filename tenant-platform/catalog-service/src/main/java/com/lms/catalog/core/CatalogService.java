package com.lms.catalog.core;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CatalogService {

    private final FeaturePolicyPort port;

    public CatalogService(FeaturePolicyPort port) {
        this.port = port;
    }

    public FeaturePolicyPort.EffectiveFeature effective(String tierId, UUID tenantId, String featureKey) {
        return port.effective(tierId, tenantId, featureKey);
    }
}
