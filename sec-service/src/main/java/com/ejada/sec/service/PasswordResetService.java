package com.ejada.sec.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.dto.ForgotPasswordRequest;
import com.ejada.sec.dto.ResetPasswordRequest;

public interface PasswordResetService {
  BaseResponse<Void> createToken(ForgotPasswordRequest req);
  BaseResponse<Void> reset(ResetPasswordRequest req);
}
