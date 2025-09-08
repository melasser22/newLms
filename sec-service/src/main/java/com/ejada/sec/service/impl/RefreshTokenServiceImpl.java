package com.ejada.sec.service.impl;

import com.ejada.sec.domain.RefreshToken;
import com.ejada.sec.domain.User;
import com.ejada.sec.repository.RefreshTokenRepository;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.service.RefreshTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

  private final RefreshTokenRepository repo;
  private final UserRepository userRepository;

  @Value("${security.refresh.ttl-seconds:2592000}") // 30 days
  private long ttlSeconds;

  @Transactional
  @Override
  public String issue(Long userId) {
    String token = UUID.randomUUID().toString();
    var now = Instant.now();
    var rt = RefreshToken.builder()
        .user(userRepository.findById(userId).orElseThrow())
        .token(token)
        .issuedAt(now)
        .expiresAt(now.plusSeconds(ttlSeconds))
        .build();
    repo.save(rt);
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
  public int revokeExpired() {
    return repo.deleteByExpiresAtBefore(Instant.now());
  }
}
