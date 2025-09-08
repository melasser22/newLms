package com.ejada.billing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Track Product Consumption Request (swagger: TrackProductConsumptionRq) */
public record TrackProductConsumptionRq(
        @NotNull Long productId,
        @NotNull @Size(min = 1) List<ProductSubscription> activeSubscriptions
) {
    public TrackProductConsumptionRq {
        activeSubscriptions = activeSubscriptions == null ? List.of() : List.copyOf(activeSubscriptions);
    }

    @Override
    public List<ProductSubscription> activeSubscriptions() {
        return List.copyOf(activeSubscriptions);
    }
}
