package com.ejada.sec.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.dto.ForgotPasswordRequest;
import com.ejada.sec.dto.ResetPasswordRequest;

public interface PasswordResetService {
  BaseResponse<String> createToken(ForgotPasswordRequest req); // returns token to deliver via email/SMS
  BaseResponse<Void> reset(ResetPasswordRequest req);
}
