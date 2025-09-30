package com.ejada.common.marketplace.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Response returned to the marketplace after processing the notification.
 */
public record ReceiveSubscriptionNotificationRs(
        @NotNull Boolean isNotificationReceived,
        List<EnvironmentIdentifierDto> environmentIdentiferLst) {

    public ReceiveSubscriptionNotificationRs {
        environmentIdentiferLst = environmentIdentiferLst == null
                ? List.of()
                : List.copyOf(environmentIdentiferLst);
    }

    @Override
    public List<EnvironmentIdentifierDto> environmentIdentiferLst() {
        return environmentIdentiferLst;
    }
}
