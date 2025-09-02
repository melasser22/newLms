package com.ejada.catalog.core;

import java.util.UUID;

public interface FeaturePolicyService {

    FeaturePolicy effective(String tierId, UUID tenantId, String featureKey);

    void upsertOverride(UUID tenantId, String featureKey, FeatureOverride override);

    record FeaturePolicy(boolean enabled,
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
