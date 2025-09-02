package com.ejada.tenant.core.adapters.shared;

import com.ejada.tenant.core.FeaturePolicyPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnClass(name = "com.shared.catalog.api.FeaturePolicyService")
@ConditionalOnMissingBean(FeaturePolicyPort.class)
public class SharedFeaturePolicyPort implements FeaturePolicyPort {
    @Override
    public EffectiveFeature effective(String tierId, UUID tenantId, String featureKey) {
        throw new UnsupportedOperationException("Shared catalog service not available");
    }
}
