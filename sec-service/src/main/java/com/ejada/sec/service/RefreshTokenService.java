package com.ejada.sec.service;

import com.ejada.sec.domain.User;

public interface RefreshTokenService {
  String issue(Long userId, boolean revokeExistingSessions, String rotatedFrom);
  User validateAndGetUser(String refreshToken);
  void revoke(String refreshToken);
  void revokeAllForUser(Long userId);
  int  revokeExpired();
}
