package com.lms.catalog.core;

import java.util.UUID;

public interface FeaturePolicyPort {

    EffectiveFeature effective(String tierId, UUID tenantId, String featureKey);

    record EffectiveFeature(boolean enabled,
                            Long limit,
                            boolean allowOverage,
                            Long overageUnitPriceMinor,
                            String overageCurrency) { }
}
