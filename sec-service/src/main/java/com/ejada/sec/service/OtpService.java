package com.ejada.sec.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.domain.MfaMethod;

public interface OtpService {

    /**
     * Generates and sends OTP code via specified method.
     *
     * @param userId user ID
     * @param method delivery method (EMAIL, SMS, CONSOLE)
     * @return response with success/failure status
     */
    BaseResponse<Void> generateAndSendOtp(Long userId, MfaMethod method);

    /**
     * Validates OTP code entered by user.
     *
     * @param userId user ID
     * @param otpCode 6-digit OTP code
     * @return true if valid
     */
    boolean validateOtp(Long userId, String otpCode);

    /**
     * Invalidates all active OTPs for user.
     */
    void invalidateUserOtps(Long userId);
}
