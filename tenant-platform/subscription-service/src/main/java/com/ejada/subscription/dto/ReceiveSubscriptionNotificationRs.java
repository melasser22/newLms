package com.ejada.subscription.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** What we return to the marketplace after processing notification. */
public record ReceiveSubscriptionNotificationRs(
    @NotNull Boolean isNotificationReceived,
    List<EnvironmentIdentifierDto> environmentIdentiferLst // optional if auto-provisioning produced identifiers
) {}
