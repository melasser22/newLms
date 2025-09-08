package com.ejada.sec.service.impl;

import com.ejada.sec.domain.User;
import com.ejada.sec.dto.*;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.service.AuthService;
import com.ejada.sec.service.PasswordResetService;
import com.ejada.sec.service.RefreshTokenService;
import com.ejada.sec.service.TokenIssuer;
import com.ejada.sec.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final UserService userService;
  private final RefreshTokenService refreshTokenService;
  private final PasswordEncoder passwordEncoder;
  private final TokenIssuer tokenIssuer; // see below

  @Transactional
  @Override
  public AuthResponse register(RegisterRequest req) {
    var created = userService.create(
        CreateUserRequest.builder()
            .tenantId(req.getTenantId())
            .username(req.getUsername())
            .email(req.getEmail())
            .password(req.getPassword())
            .roles(req.getRoles())
            .build()
    );
    return issueTokens(created.getTenantId(), created.getUsername(), created.getId());
  }

  @Transactional
  @Override
  public AuthResponse login(AuthRequest req) {
    UUID tenantId = req.getTenantId();
    // identifier can be username or email
    User user = userRepository.findByTenantIdAndUsername(tenantId, req.getIdentifier())
        .or(() -> userRepository.findByTenantIdAndEmail(tenantId, req.getIdentifier()))
        .orElseThrow(() -> new NoSuchElementException("Invalid credentials"));

    if (!user.isEnabled() || user.isLocked()) {
      throw new IllegalStateException("Account disabled or locked");
    }
    if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
      throw new NoSuchElementException("Invalid credentials");
    }
    return issueTokens(user.getTenantId(), user.getUsername(), user.getId());
  }

  @Transactional
  @Override
  public AuthResponse refresh(RefreshTokenRequest req) {
    var user = refreshTokenService.validateAndGetUser(req.getRefreshToken());
    return issueTokens(user.getTenantId(), user.getUsername(), user.getId());
  }

  @Transactional
  @Override
  public void logout(String refreshToken) {
    refreshTokenService.revoke(refreshToken);
  }

  private AuthResponse issueTokens(UUID tenantId, String username, Long userId) {
    String access = tokenIssuer.issueAccessToken(tenantId, userId, username);
    String refresh = refreshTokenService.issue(userId);
    return AuthResponse.builder()
        .accessToken(access)
        .refreshToken(refresh)
        .tokenType("Bearer")
        .expiresInSeconds(tokenIssuer.getAccessTokenTtlSeconds())
        .build();
  }
}
