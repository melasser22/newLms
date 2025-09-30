package com.ejada.common.marketplace.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Subscription information received from the marketplace. Collection fields are
 * defensively copied to avoid exposing internal mutable state.
 */
public record SubscriptionInfoDto(
        @NotNull Long subscriptionId,
        @NotNull Long customerId,
        @NotNull Long productId,
        @NotNull Long tierId,
        String tierNameEn,
        String tierNameAr,
        @NotNull LocalDate startDt,
        @NotNull LocalDate endDt,
        BigDecimal subscriptionAmount,
        BigDecimal totalBilledAmount,
        BigDecimal totalPaidAmount,
        @NotBlank String subscriptionSttsCd,
        String createChannel,
        Boolean unlimitedUsersFlag,
        Long usersLimit,
        String usersLimitResetType,
        Boolean unlimitedTransFlag,
        Long transactionsLimit,
        String transLimitResetType,
        BigDecimal balanceLimit,
        String balanceLimitResetType,
        String environmentSizeCd,
        Boolean isAutoProvEnabled,
        Long prevSubscriptionId,
        String prevSubscriptionUpdateAction,
        @Valid List<SubscriptionFeatureDto> subscriptionFeatureLst,
        @Valid List<SubscriptionAdditionalServiceDto> subscriptionAdditionalServicesLst) {

    public SubscriptionInfoDto {
        subscriptionFeatureLst = subscriptionFeatureLst == null
                ? List.of()
                : List.copyOf(subscriptionFeatureLst);
        subscriptionAdditionalServicesLst = subscriptionAdditionalServicesLst == null
                ? List.of()
                : List.copyOf(subscriptionAdditionalServicesLst);
    }

    @Override
    public List<SubscriptionFeatureDto> subscriptionFeatureLst() {
        return subscriptionFeatureLst;
    }

    @Override
    public List<SubscriptionAdditionalServiceDto> subscriptionAdditionalServicesLst() {
        return subscriptionAdditionalServicesLst;
    }
}
