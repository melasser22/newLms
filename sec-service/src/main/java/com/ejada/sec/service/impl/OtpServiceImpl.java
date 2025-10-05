package com.ejada.sec.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.crypto.password.PasswordHasher;
import com.ejada.sec.domain.MfaMethod;
import com.ejada.sec.domain.OtpCode;
import com.ejada.sec.domain.User;
import com.ejada.sec.notification.NotificationChannel;
import com.ejada.sec.notification.NotificationResult;
import com.ejada.sec.repository.OtpCodeRepository;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final UserRepository userRepository;
    private final List<NotificationChannel> notificationChannels;

    @Value("${sec.mfa.otp.length:6}")
    private int otpLength;

    @Value("${sec.mfa.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${sec.mfa.otp.max-attempts:3}")
    private int maxAttempts;

    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public BaseResponse<Void> generateAndSendOtp(Long userId, MfaMethod method) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (!method.isOtpBased()) {
            return BaseResponse.error("INVALID_METHOD",
                "Method does not support OTP: " + method);
        }

        NotificationChannel channel = getNotificationChannel(method);
        if (channel == null || !channel.isEnabled()) {
            return BaseResponse.error("CHANNEL_DISABLED",
                "Notification channel not available: " + method);
        }

        invalidateUserOtps(userId);

        String otpCode = generateOtpCode();
        String otpHash = PasswordHasher.bcrypt(otpCode);

        String recipient = getRecipient(user, method);
        if (recipient == null || recipient.isBlank()) {
            return BaseResponse.error("NO_RECIPIENT",
                "No recipient configured for method: " + method);
        }

        NotificationResult result = channel.send(recipient, otpCode, otpExpiryMinutes);

        OtpCode otp = OtpCode.builder()
            .user(user)
            .codeHash(otpHash)
            .method(method)
            .generatedAt(Instant.now())
            .expiresAt(Instant.now().plus(otpExpiryMinutes, ChronoUnit.MINUTES))
            .sentTo(recipient)
            .delivered(result.isSuccess())
            .deliveryError(result.getErrorDetails())
            .build();

        otpCodeRepository.save(otp);

        if (!result.isSuccess()) {
            log.error("Failed to send OTP to {} via {}: {}",
                recipient, method, result.getErrorDetails());
            return BaseResponse.error("SEND_FAILED",
                "Failed to send OTP: " + result.getErrorDetails());
        }

        log.info("OTP generated and sent to user {} via {}", userId, method);
        return BaseResponse.success("OTP sent successfully", null);
    }

    @Override
    @Transactional
    public boolean validateOtp(Long userId, String otpCode) {
        userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));

        List<OtpCode> activeOtps = otpCodeRepository.findActiveOtpsByUserId(
            userId, Instant.now());

        if (activeOtps.isEmpty()) {
            log.warn("No active OTP found for user: {}", userId);
            return false;
        }

        boolean valid = false;

        for (OtpCode otp : activeOtps) {
            if (PasswordHasher.matchesBcrypt(otpCode, otp.getCodeHash())) {
                otp.markAsUsed();
                otpCodeRepository.save(otp);
                valid = true;
                break;
            }

            otp.incrementFailedAttempts();
            if (otp.getFailedAttempts() >= maxAttempts) {
                otp.markAsUsed();
                log.warn("OTP invalidated due to {} failed attempts for user: {}",
                    maxAttempts, userId);
            }
            otpCodeRepository.save(otp);
        }

        if (!valid) {
            log.warn("Invalid OTP code for user: {}", userId);
        }

        return valid;
    }

    @Override
    @Transactional
    public void invalidateUserOtps(Long userId) {
        List<OtpCode> activeOtps = otpCodeRepository.findActiveOtpsByUserId(
            userId, Instant.now());

        activeOtps.forEach(OtpCode::markAsUsed);
        otpCodeRepository.saveAll(activeOtps);

        log.debug("Invalidated {} active OTPs for user: {}", activeOtps.size(), userId);
    }

    private String generateOtpCode() {
        int bound = (int) Math.pow(10, otpLength);
        int code = random.nextInt(bound);
        return String.format("%0" + otpLength + "d", code);
    }

    private NotificationChannel getNotificationChannel(MfaMethod method) {
        Map<MfaMethod, NotificationChannel> channelMap = notificationChannels.stream()
            .collect(Collectors.toMap(
                NotificationChannel::getSupportedMethod,
                Function.identity(),
                (existing, replacement) -> existing
            ));

        return channelMap.get(method);
    }

    private String getRecipient(User user, MfaMethod method) {
        return switch (method) {
            case EMAIL, CONSOLE -> user.getEmail();
            case SMS -> user.getPhoneNumber();
            default -> null;
        };
    }
}
