package com.ejada.sec.notification;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResult {
    private boolean success;
    private String message;
    private String errorDetails;

    public static NotificationResult success(String message) {
        return NotificationResult.builder()
            .success(true)
            .message(message)
            .build();
    }

    public static NotificationResult failure(String errorDetails) {
        return NotificationResult.builder()
            .success(false)
            .errorDetails(errorDetails)
            .build();
    }
}
