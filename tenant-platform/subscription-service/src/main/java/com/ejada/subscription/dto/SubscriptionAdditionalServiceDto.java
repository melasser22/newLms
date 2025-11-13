package com.ejada.subscription.dto;

import com.ejada.subscription.json.YesNoBooleanDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

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

    @JsonDeserialize(using = YesNoBooleanDeserializer.class)
    Boolean isCountable,    // entity will persist Y/N
    Long requestedCount,

    String paymentTypeCd    // ONE_TIME_FEES | WITH_INSTALLMENT
) { }