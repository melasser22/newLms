package com.ejada.common.events.provisioning;

import java.math.BigDecimal;

/** Representation of an addon assigned to a tenant subscription. */
public record ProvisionedAddon(
        Long productAdditionalServiceId,
        String code,
        String nameEn,
        String nameAr,
        BigDecimal servicePrice,
        BigDecimal totalAmount,
        String currency,
        Boolean countable,
        Long requestedCount,
        String paymentType) {
}
