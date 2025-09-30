package com.ejada.common.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Additional services bundled with a marketplace subscription.
 */
public record SubscriptionAdditionalServiceDto(
        @NotNull Long productAdditionalServiceId,
        @NotBlank String serviceCd,
        @NotBlank String serviceNameEn,
        @NotBlank String serviceNameAr,
        String serviceDescEn,
        String serviceDescAr,
        BigDecimal servicePrice,
        BigDecimal totalAmount,
        String currency,
        Boolean isCountable,
        Long requestedCount,
        String paymentTypeCd) {
}
