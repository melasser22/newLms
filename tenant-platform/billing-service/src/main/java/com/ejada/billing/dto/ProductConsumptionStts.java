package com.ejada.billing.dto;

import jakarta.validation.constraints.NotNull;

/** swagger: ProductConsumptionStts */
public record ProductConsumptionStts(
        @NotNull ConsumptionType consumptionTypCd,
        Long   currentConsumption,     // only for TRANSACTION/USER
        Double currentConsumedAmount   // only for BALANCE
) {}
