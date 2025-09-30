package com.ejada.common.marketplace.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO representing a subscription notification request coming from the marketplace.
 */
public record ReceiveSubscriptionNotificationRq(
        @NotNull @Valid CustomerInfoDto customerInfo,
        @NotNull @Valid AdminUserInfoDto adminUserInfo,
        @NotNull @Valid SubscriptionInfoDto subscriptionInfo,
        @Valid List<ProductPropertyDto> productProperties) {

    public ReceiveSubscriptionNotificationRq {
        productProperties = productProperties == null ? List.of() : List.copyOf(productProperties);
    }

    @Override
    public List<ProductPropertyDto> productProperties() {
        return productProperties;
    }
}
