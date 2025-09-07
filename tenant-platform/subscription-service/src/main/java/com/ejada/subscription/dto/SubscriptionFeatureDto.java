package com.ejada.subscription.dto;

import jakarta.validation.constraints.*;

public record SubscriptionFeatureDto(
    @NotBlank String featureCd,
    Integer featureCount
) {}