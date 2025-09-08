package com.ejada.sec.service;

import com.ejada.sec.domain.User;

public interface RefreshTokenService {
  String issue(Long userId);
  User validateAndGetUser(String refreshToken);
  void revoke(String refreshToken);
  int  revokeExpired();
}
