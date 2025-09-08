package com.ejada.sec.service;

import com.ejada.sec.dto.*;

public interface AuthService {
  AuthResponse register(RegisterRequest req);
  AuthResponse login(AuthRequest req);
  AuthResponse refresh(RefreshTokenRequest req);
  void logout(String refreshToken); // revoke
}
