package com.ejada.billing.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Track Product Consumption Response (swagger: TrackProductConsumptionRs) */
public record TrackProductConsumptionRs(
        @NotNull Long productId,
        List<ProductSubscriptionStts> subscriptionsStts
) {
    public TrackProductConsumptionRs {
        subscriptionsStts = subscriptionsStts == null ? List.of() : List.copyOf(subscriptionsStts);
    }

    @Override
    public List<ProductSubscriptionStts> subscriptionsStts() {
        return List.copyOf(subscriptionsStts);
    }
}
