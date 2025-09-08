package com.ejada.sec.service;

import com.ejada.sec.dto.ForgotPasswordRequest;
import com.ejada.sec.dto.ResetPasswordRequest;

public interface PasswordResetService {
  String createToken(ForgotPasswordRequest req); // returns token to deliver via email/SMS
  void reset(ResetPasswordRequest req);
}
