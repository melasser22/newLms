package com.ejada.subscription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ReceiveSubscriptionNotificationRq(
    @NotNull @Valid CustomerInfoDto customerInfo,
    @NotNull @Valid AdminUserInfoDto adminUserInfo,
    @NotNull @Valid SubscriptionInfoDto subscriptionInfo,
    @Valid List<ProductPropertyDto> productProperties
) {}
