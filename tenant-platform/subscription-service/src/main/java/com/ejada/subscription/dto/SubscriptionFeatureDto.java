package com.ejada.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionFeatureDto(
    @NotBlank String featureCd,
    Integer featureCount
) { }