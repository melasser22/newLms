package com.ejada.billing.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** swagger: ProductSubscriptionStts */
public record ProductSubscriptionStts(
        @NotNull Long customerId,
        @NotNull Long subscriptionId,
        List<ProductConsumptionStts> productConsumptionStts
) {}
