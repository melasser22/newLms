package com.ejada.catalog.service;

import java.util.UUID;

/**
 * Port exposing feature policy operations.
 */
public interface FeaturePolicyPort {

    EffectiveFeature effective(String tierId, UUID tenantId, String featureKey);

    void upsertOverride(UUID tenantId, String featureKey, FeatureOverride override);

    record EffectiveFeature(boolean enabled,
                            Long limit,
                            boolean allowOverage,
                            Long overageUnitPriceMinor,
                            String overageCurrency) { }

    record FeatureOverride(Boolean enabled,
                           Long limit,
                           Boolean allowOverage,
                           Long overageUnitPriceMinor,
                           String overageCurrency) { }
}
