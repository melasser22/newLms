package com.ejada.catalog.service;

import com.ejada.catalog.entity.TenantFeatureOverrideEntity;
import com.ejada.catalog.entity.TierFeatureLimitEntity;
import com.ejada.catalog.repository.TenantFeatureOverrideRepository;
import com.ejada.catalog.repository.TierFeatureLimitRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Transactional
public class EffectiveFeatureResolver implements FeaturePolicyPort {

    private final TierFeatureLimitRepository tierRepository;
    private final TenantFeatureOverrideRepository overrideRepository;
    private final EntityManager entityManager;

    public EffectiveFeatureResolver(TierFeatureLimitRepository tierRepository, TenantFeatureOverrideRepository overrideRepository,
                                    EntityManager entityManager) {
        this.tierRepository = tierRepository;
        this.overrideRepository = overrideRepository;
        this.entityManager = entityManager;
    }

    @Override
    public EffectiveFeature effective(String tierId, UUID tenantId, String featureKey) {
        setTenant(tenantId);
        TierFeatureLimitEntity base = tierRepository.findByIdTierIdAndIdFeatureKey(tierId, featureKey)
                .orElseThrow();
        TenantFeatureOverrideEntity override =
                overrideRepository.findByIdTenantIdAndIdFeatureKey(tenantId, featureKey).orElse(null);
        boolean enabled = override != null && override.getEnabled() != null ? override.getEnabled() : Boolean.TRUE.equals(base.getEnabled());
        Long limit = override != null && override.getLimitValue() != null ? override.getLimitValue() : base.getLimitValue();
        boolean allowOverage = override != null && override.getAllowOverageOverride() != null ? override.getAllowOverageOverride() : Boolean.TRUE.equals(base.getAllowOverage());
        Long overagePrice = override != null && override.getOverageUnitPriceMinorOverride() != null ? override.getOverageUnitPriceMinorOverride() : base.getOverageUnitPriceMinor();
        String overageCurrency = override != null && override.getOverageCurrencyOverride() != null ? override.getOverageCurrencyOverride() : base.getOverageCurrency();
        return new EffectiveFeature(enabled, limit, allowOverage, overagePrice, overageCurrency);
    }

    @Override
    public void upsertOverride(UUID tenantId, String featureKey, FeatureOverride override) {
        setTenant(tenantId);
        overrideRepository.upsert(tenantId, featureKey, override.enabled(), override.limit(),
                override.allowOverage(), override.overageUnitPriceMinor(), override.overageCurrency());
    }

    private void setTenant(UUID tenantId) {
        entityManager.createNativeQuery("set app.current_tenant = :tenant")
                .setParameter("tenant", tenantId.toString())
                .executeUpdate();
    }
}
