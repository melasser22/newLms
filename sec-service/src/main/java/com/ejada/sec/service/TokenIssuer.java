package com.ejada.sec.service;

import java.util.UUID;

public interface TokenIssuer {
  String issueAccessToken(UUID tenantId, Long userId, String username);
  long getAccessTokenTtlSeconds();
}
