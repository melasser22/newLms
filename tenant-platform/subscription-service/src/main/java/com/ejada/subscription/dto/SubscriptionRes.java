package com.ejada.subscription.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SubscriptionRes(
    Long subscriptionId,
    Long extSubscriptionId,
    Long extCustomerId,
    Long extProductId,
    Long extTierId,
    String tierNmEn,
    String tierNmAr,
    LocalDate startDt,
    LocalDate endDt,
    BigDecimal subscriptionAmount,
    BigDecimal totalBilledAmount,
    BigDecimal totalPaidAmount,
    String subscriptionSttsCd,
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
    Boolean isAutoProvEnabled
) {}
