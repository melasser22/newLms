package com.ejada.billing.dto;

import jakarta.validation.constraints.NotNull;

/** swagger: ProductConsumption */
public record ProductConsumption(
        @NotNull ConsumptionType consumptionTypCd
) { }
