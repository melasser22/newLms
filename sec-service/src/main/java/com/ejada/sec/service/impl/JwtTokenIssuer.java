package com.ejada.sec.service.impl;

import com.ejada.crypto.JwtTokenService;
import com.ejada.sec.service.TokenIssuer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenIssuer implements TokenIssuer {

  private final JwtTokenService jwtTokenService;

  @Value("${security.jwt.issuer:sec-service}")
  private String issuer;

  @Value("${security.jwt.access-ttl-seconds:1800}")
  private long accessTtlSeconds;

  @Override
  public String issueAccessToken(UUID tenantId, Long userId, String username) {
    Map<String, Object> claims =
        Map.of("iss", issuer, "uid", userId, "tid", tenantId.toString());
    return jwtTokenService.createToken(
        username, tenantId.toString(), List.of(), claims, Duration.ofSeconds(accessTtlSeconds));
  }

  @Override
  public long getAccessTokenTtlSeconds() {
    return accessTtlSeconds;
  }
}

