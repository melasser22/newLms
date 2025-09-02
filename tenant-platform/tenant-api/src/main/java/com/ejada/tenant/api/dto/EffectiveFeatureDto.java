package com.ejada.tenant.api.dto;

/**
 * Represents the effective configuration of a feature for a tenant after
 * all policies and subscriptions have been evaluated.
 *
 * @param featureCode identifier for the feature
 * @param enabled     whether the feature is enabled
 * @param limit       optional limit for the feature (null if unlimited)
 */
public record EffectiveFeatureDto(String featureCode, boolean enabled, Long limit) {
}

