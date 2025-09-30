package com.ejada.common.marketplace.subscription.dto;

import jakarta.validation.constraints.NotNull;

/** Update event from marketplace: suspend/resume/terminate/expire. */
public record ReceiveSubscriptionUpdateRq(
        @NotNull Long subscriptionId,
        @NotNull Long customerId,
        @NotNull SubscriptionUpdateType subscriptionUpdateType) {
}
