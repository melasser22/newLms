package com.ejada.sec.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.dto.*;

public interface AuthService {
  BaseResponse<AuthResponse> register(RegisterRequest req);
  BaseResponse<AuthResponse> login(AuthRequest req);
  BaseResponse<AuthResponse> refresh(RefreshTokenRequest req);
  BaseResponse<Void> logout(String refreshToken); // revoke
  BaseResponse<Void> completeFirstLogin(CompleteFirstLoginRequest req);
}
