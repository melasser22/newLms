package com.ejada.billing.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** swagger: ProductSubscription */
public record ProductSubscription(
        @NotNull Long customerId,
        @NotNull Long subscriptionId,
        String startDt,   // ISO-8601 date string per swagger examples
        String endDt,     // ISO-8601 date string per swagger examples
        List<ProductConsumption> productConsumption
) {}
