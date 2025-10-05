package com.ejada.sec.notification;

import com.ejada.sec.domain.MfaMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Console-based OTP delivery for DEVELOPMENT ONLY.
 * Prints OTP codes to console/logs for easy testing.
 *
 * WARNING: Never enable this in production!
 */
@Component
@Profile({"dev", "local", "test"})
@Slf4j
public class ConsoleNotificationChannel implements NotificationChannel {

    @Value("${sec.mfa.console.enabled:true}")
    private boolean enabled;

    @Override
    public NotificationResult send(String recipient, String otpCode, int expiryMinutes) {
        if (!enabled) {
            return NotificationResult.failure("Console notification disabled");
        }

        String border = "═".repeat(70);

        log.info("\n");
        log.info(border);
        log.info("║  🔐 MFA VERIFICATION CODE (DEVELOPMENT MODE)");
        log.info("║");
        log.info("║  Recipient: {}", recipient);
        log.info("║  OTP Code:  {}", otpCode);
        log.info("║  Valid for: {} minutes", expiryMinutes);
        log.info("║");
        log.info("║  ⚠️  This is DEVELOPMENT mode - code printed to console");
        log.info(border);
        log.info("\n");

        return NotificationResult.success("OTP printed to console");
    }

    @Override
    public MfaMethod getSupportedMethod() {
        return MfaMethod.CONSOLE;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
