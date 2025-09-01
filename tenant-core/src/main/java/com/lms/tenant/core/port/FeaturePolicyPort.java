package com.lms.tenant.core.port;

import java.util.UUID;

/** Port to resolve effective feature policy. */
public interface FeaturePolicyPort {

    record EffectiveFeature(boolean enabled, Long limit, boolean allowOverage, Long overageUnitPriceMinor, String overageCurrency) {}

    EffectiveFeature effective(String tierId, UUID tenantId, String featureKey);
}
