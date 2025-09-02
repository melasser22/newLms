package com.ejada.tenant.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Represents a tenant's subscription plan.
 *
 * @param id        subscription identifier
 * @param plan      name of the subscribed plan
 * @param startDate date the subscription became active
 * @param endDate   optional end date (null if active)
 * @param features  features included with the subscription
 */
public record SubscriptionDto(
    UUID id,
    String plan,
    LocalDate startDate,
    LocalDate endDate,
    List<EffectiveFeatureDto> features
) {
}

