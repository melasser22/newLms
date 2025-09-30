package com.ejada.common.marketplace.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Feature entitlements provisioned with the subscription.
 */
public record SubscriptionFeatureDto(
        @NotBlank String featureCd,
        Integer featureCount) {
}
