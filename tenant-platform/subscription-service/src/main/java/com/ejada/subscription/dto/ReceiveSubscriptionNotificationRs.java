package com.ejada.subscription.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Response returned to the marketplace after processing the notification. The
 * list of environment identifiers is copied to maintain immutability.
 */
public record ReceiveSubscriptionNotificationRs(
        @NotNull Boolean isNotificationReceived,
        List<EnvironmentIdentifierDto> environmentIdentiferLst) {

    public ReceiveSubscriptionNotificationRs {
        environmentIdentiferLst =
                environmentIdentiferLst == null
                        ? List.of()
                        : List.copyOf(environmentIdentiferLst);
    }

    public List<EnvironmentIdentifierDto> environmentIdentiferLst() {
        return environmentIdentiferLst;
    }
}

