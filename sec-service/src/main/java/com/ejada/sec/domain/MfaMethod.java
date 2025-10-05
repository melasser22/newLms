package com.ejada.sec.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Supported MFA methods with notification channels.
 */
@Getter
@RequiredArgsConstructor
public enum MfaMethod {
    /**
     * Time-based OTP via authenticator app (Google Authenticator, Authy).
     */
    TOTP("Authenticator App", false),

    /**
     * OTP sent via email.
     */
    EMAIL("Email Verification", true),

    /**
     * OTP sent via SMS.
     */
    SMS("SMS Verification", true),

    /**
     * OTP printed to console (DEVELOPMENT ONLY).
     */
    CONSOLE("Console Output (Dev)", true);

    private final String description;
    private final boolean requiresOtpGeneration;

    /**
     * Checks if this method requires OTP code generation and delivery.
     */
    public boolean isOtpBased() {
        return requiresOtpGeneration;
    }

    /**
     * Checks if this method is allowed in production.
     */
    public boolean isProductionSafe() {
        return this != CONSOLE;
    }
}
