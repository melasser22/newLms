package com.ejada.catalog.service;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

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
                           @PositiveOrZero Long limit,
                           Boolean allowOverage,
                           @PositiveOrZero Long overageUnitPriceMinor,
                           @NotBlank @Size(max = 3) String overageCurrency) { }
}
