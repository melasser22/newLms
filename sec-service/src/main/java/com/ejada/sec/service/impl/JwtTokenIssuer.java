package com.ejada.sec.service.impl;

import com.ejada.sec.service.TokenIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

// You can replace this with your shared JwtTokenService wrapper
@Service
@RequiredArgsConstructor
public class JwtTokenIssuer implements TokenIssuer {

  @Value("${security.jwt.issuer:sec-service}")
  private String issuer;

  @Value("${security.jwt.access-ttl-seconds:1800}")
  private long accessTtlSeconds;

  @Override
  public String issueAccessToken(UUID tenantId, Long userId, String username) {
    // Delegate to your shared JwtTokenService; here’s a placeholder:
    // return jwtTokenService.createAccessToken(tenantId, userId, username, accessTtlSeconds);
    return JwtStub.create(issuer, tenantId, userId, username, accessTtlSeconds);
  }

  @Override
  public long getAccessTokenTtlSeconds() {
    return accessTtlSeconds;
  }

  // --- dummy inner class for illustration; replace with your shared lib ---
  static class JwtStub {
    static String create(String iss, UUID tid, Long uid, String sub, long ttl) {
      return "stub.jwt." + iss + "." + tid + "." + uid + "." + sub + "." + (Instant.now().getEpochSecond()+ttl);
    }
  }
}
