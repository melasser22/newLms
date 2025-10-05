package com.ejada.sec.notification;

import com.ejada.sec.domain.MfaMethod;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * SMS-based OTP delivery using Twilio.
 * Only enabled if Twilio is on classpath and configured.
 */
@Component
@ConditionalOnClass(Twilio.class)
@Slf4j
public class SmsNotificationChannel implements NotificationChannel {

    @Value("${sec.mfa.sms.enabled:false}")
    private boolean enabled;

    @Value("${sec.mfa.sms.twilio.account-sid:}")
    private String accountSid;

    @Value("${sec.mfa.sms.twilio.auth-token:}")
    private String authToken;

    @Value("${sec.mfa.sms.twilio.from-number:}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        if (enabled && accountSid != null && !accountSid.isBlank() && authToken != null && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SMS channel initialized");
        }
    }

    @Override
    public NotificationResult send(String recipient, String otpCode, int expiryMinutes) {
        if (!enabled) {
            return NotificationResult.failure("SMS notification disabled");
        }

        try {
            String messageBody = String.format(
                "Your verification code is: %s\n\nValid for %d minutes.\n\nIf you didn't request this, please ignore.",
                otpCode, expiryMinutes
            );

            Message message = Message.creator(
                new PhoneNumber(recipient),
                new PhoneNumber(fromNumber),
                messageBody
            ).create();

            log.info("OTP SMS sent successfully to: {} (SID: {})", recipient, message.getSid());

            return NotificationResult.success("OTP sent via SMS");

        } catch (Exception e) {
            log.error("Failed to send OTP SMS to: {}", recipient, e);
            return NotificationResult.failure("SMS sending failed: " + e.getMessage());
        }
    }

    @Override
    public MfaMethod getSupportedMethod() {
        return MfaMethod.SMS;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
