package com.ejada.sec.notification;

import com.ejada.sec.domain.MfaMethod;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Email-based OTP delivery using Spring Mail.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationChannel implements NotificationChannel {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${sec.mfa.email.enabled:true}")
    private boolean enabled;

    @Value("${sec.mfa.email.from:noreply@yourcompany.com}")
    private String fromEmail;

    @Value("${sec.mfa.email.subject:Your Verification Code}")
    private String subject;

    @Override
    public NotificationResult send(String recipient, String otpCode, int expiryMinutes) {
        if (!enabled) {
            return NotificationResult.failure("Email notification disabled");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(recipient);
            helper.setSubject(subject);

            String htmlContent = generateEmailContent(otpCode, expiryMinutes);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("OTP email sent successfully to: {}", recipient);
            return NotificationResult.success("OTP sent to email");

        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {}", recipient, e);
            return NotificationResult.failure("Email sending failed: " + e.getMessage());
        }
    }

    @Override
    public MfaMethod getSupportedMethod() {
        return MfaMethod.EMAIL;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private String generateEmailContent(String otpCode, int expiryMinutes) {
        Context context = new Context();
        context.setVariable("otpCode", otpCode);
        context.setVariable("expiryMinutes", expiryMinutes);
        context.setVariable("year", java.time.Year.now().getValue());

        return templateEngine.process("email/otp-code", context);
    }
}
