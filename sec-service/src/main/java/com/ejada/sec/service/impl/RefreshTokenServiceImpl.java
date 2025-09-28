package com.ejada.sec.service.impl;

import com.ejada.sec.domain.RefreshToken;
import com.ejada.sec.domain.User;
import com.ejada.sec.repository.RefreshTokenRepository;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.service.RefreshTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

  private final RefreshTokenRepository repo;
  private final UserRepository userRepository;

  @Value("${security.refresh.ttl-seconds:2592000}") // 30 days
  private long ttlSeconds;

  @Value("${security.refresh.max-active-per-user:5}")
  private int maxActivePerUser;

  @Value("${security.refresh.max-active-per-tenant:1000}")
  private int maxActivePerTenant;

  @Transactional
  @Override
  public String issue(Long userId, boolean revokeExistingSessions, String rotatedFrom) {
    var now = Instant.now();
    var user = userRepository.findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

    if (revokeExistingSessions) {
      var revoked = revokeTokens(repo.findActiveTokensByUserId(userId, now), now);
      if (revoked > 0) {
        log.debug("Revoked {} active refresh tokens for user {} prior to issuing a new session", revoked, userId);
      }
    }

    enforceTenantLimit(user.getTenantId(), now);

    String token = UUID.randomUUID().toString();
    var rt = RefreshToken.builder()
        .user(user)
        .token(token)
        .issuedAt(now)
        .expiresAt(now.plusSeconds(ttlSeconds))
        .rotatedFrom(rotatedFrom)
        .build();
    repo.save(rt);

    enforceUserLimit(userId, now);

    return token;
  }

  @Override
  public User validateAndGetUser(String refreshToken) {
    var now = Instant.now();
    var rt = repo.findByToken(refreshToken)
        .filter(t -> t.getRevokedAt() == null && t.getExpiresAt().isAfter(now))
        .orElseThrow(() -> new NoSuchElementException("Invalid or expired refresh token"));
    return rt.getUser();
  }

  @Transactional
  @Override
  public void revoke(String refreshToken) {
    repo.findByToken(refreshToken).ifPresent(t -> {
      t.setRevokedAt(Instant.now());
      repo.save(t);
    });
  }

  @Transactional
  @Override
  public void revokeAllForUser(Long userId) {
    var now = Instant.now();
    var revoked = revokeTokens(repo.findActiveTokensByUserId(userId, now), now);
    if (revoked > 0) {
      log.debug("Revoked {} active refresh tokens for user {}", revoked, userId);
    }
  }

  @Transactional
  @Override
  public int revokeExpired() {
    return repo.deleteByExpiresAtBefore(Instant.now());
  }

  private void enforceUserLimit(Long userId, Instant now) {
    List<RefreshToken> active = repo.findActiveTokensByUserId(userId, now);
    if (maxActivePerUser <= 0) {
      return;
    }

    if (active.size() <= maxActivePerUser) {
      return;
    }

    active.sort(Comparator.comparing(RefreshToken::getIssuedAt).reversed());
    revokeTokens(active.subList(maxActivePerUser, active.size()), now);
  }

  private void enforceTenantLimit(UUID tenantId, Instant now) {
    if (maxActivePerTenant <= 0) {
      return;
    }
    List<RefreshToken> active = repo.findActiveTokensByTenant(tenantId, now);
    int capacity = Math.max(maxActivePerTenant - 1, 0);
    int toCull = active.size() - capacity;
    if (toCull <= 0) {
      return;
    }
    active.sort(Comparator.comparing(RefreshToken::getIssuedAt));
    revokeTokens(active.subList(0, toCull), now);
  }

  private int revokeTokens(List<RefreshToken> tokens, Instant when) {
    int count = 0;
    for (RefreshToken token : tokens) {
      if (token.getRevokedAt() == null && token.getExpiresAt().isAfter(when)) {
        token.setRevokedAt(when);
        repo.save(token);
        count++;
      }
    }
    return count;
  }
}
