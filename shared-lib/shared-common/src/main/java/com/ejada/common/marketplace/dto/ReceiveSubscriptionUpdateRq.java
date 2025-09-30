package com.ejada.common.marketplace.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Update event from the marketplace: suspend/resume/terminate/expire.
 */
public record ReceiveSubscriptionUpdateRq(
        @NotNull Long subscriptionId,
        @NotNull Long customerId,
        @NotNull SubscriptionUpdateType subscriptionUpdateType) {
}
