package com.ejada.sec.notification;

import com.ejada.sec.domain.MfaMethod;

/**
 * Interface for OTP notification delivery channels.
 */
public interface NotificationChannel {

    /**
     * Sends OTP code to the recipient.
     *
     * @param recipient email address, phone number, or user identifier
     * @param otpCode the 6-digit OTP code
     * @param expiryMinutes how long the code is valid
     * @return result of sending notification
     */
    NotificationResult send(String recipient, String otpCode, int expiryMinutes);

    /**
     * Gets the MFA method this channel supports.
     */
    MfaMethod getSupportedMethod();

    /**
     * Checks if this channel is enabled in current environment.
     */
    boolean isEnabled();
}
