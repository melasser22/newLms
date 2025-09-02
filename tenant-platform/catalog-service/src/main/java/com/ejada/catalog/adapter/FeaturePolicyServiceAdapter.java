package com.ejada.catalog.adapter;

import com.ejada.catalog.service.FeaturePolicyPort;
import com.ejada.catalog.service.FeaturePolicyService;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FeaturePolicyServiceAdapter implements FeaturePolicyService {

    private final FeaturePolicyPort port;

    public FeaturePolicyServiceAdapter(FeaturePolicyPort port) {
        this.port = port;
    }

    @Override
    public FeaturePolicy effective(String tierId, UUID tenantId, String featureKey) {
        var ef = port.effective(tierId, tenantId, featureKey);
        return new FeaturePolicy(ef.enabled(), ef.limit(), ef.allowOverage(), ef.overageUnitPriceMinor(), ef.overageCurrency());
    }

    @Override
    public void upsertOverride(UUID tenantId, String featureKey, FeatureOverride override) {
        port.upsertOverride(tenantId, featureKey, new FeaturePolicyPort.FeatureOverride(
                override.enabled(), override.limit(), override.allowOverage(), override.overageUnitPriceMinor(), override.overageCurrency()
        ));
    }
}
