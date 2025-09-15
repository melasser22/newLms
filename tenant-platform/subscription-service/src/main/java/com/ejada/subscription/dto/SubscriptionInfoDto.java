package com.ejada.subscription.dto;

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

        @NotBlank String subscriptionSttsCd, // marketplace status code (string)
        String createChannel, // optional (PORTAL | GCP_MARKETPLACE)

        // Limits (we use Boolean here; entities handle Y/N conversion)
        Boolean unlimitedUsersFlag,
        Long usersLimit,
        String usersLimitResetType, // FULL_SUBSCRIPTION_PERIOD | PAYMENT_FREQUENCY_PERIOD

        Boolean unlimitedTransFlag,
        Long transactionsLimit,
        String transLimitResetType,

        BigDecimal balanceLimit,
        String balanceLimitResetType,

        String environmentSizeCd, // L | XL

        Boolean isAutoProvEnabled,
        Long prevSubscriptionId,
        String prevSubscriptionUpdateAction, // UPGRADE | DOWNGRADE | RENEWAL

        @Valid List<SubscriptionFeatureDto> subscriptionFeatureLst,
        @Valid List<SubscriptionAdditionalServiceDto> subscriptionAdditionalServicesLst) {

    public SubscriptionInfoDto {
        subscriptionFeatureLst =
                subscriptionFeatureLst == null
                        ? List.of()
                        : List.copyOf(subscriptionFeatureLst);
        subscriptionAdditionalServicesLst =
                subscriptionAdditionalServicesLst == null
                        ? List.of()
                        : List.copyOf(subscriptionAdditionalServicesLst);
    }

    public List<SubscriptionFeatureDto> subscriptionFeatureLst() {
        return subscriptionFeatureLst;
    }

    public List<SubscriptionAdditionalServiceDto> subscriptionAdditionalServicesLst() {
        return subscriptionAdditionalServicesLst;
    }
}

