package com.ejada.sec.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.crypto.password.PasswordHasher;
import com.ejada.sec.domain.User;
import com.ejada.sec.dto.*;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.service.AuthService;
import com.ejada.sec.service.RefreshTokenService;
import com.ejada.sec.service.TokenIssuer;
import com.ejada.sec.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final UserService userService;
  private final RefreshTokenService refreshTokenService;
  private final TokenIssuer tokenIssuer; // see below

  @Transactional
  @Override
  public BaseResponse<AuthResponse> register(RegisterRequest req) {
    log.info("Registering user '{}' for tenant {}", req.getUsername(), req.getTenantId());
    var created = userService.create(
        CreateUserRequest.builder()
            .tenantId(req.getTenantId())
            .username(req.getUsername())
            .email(req.getEmail())
            .password(req.getPassword())
            .roles(req.getRoles())
            .build()
    ).getData();
    var tokens = issueTokens(created.getTenantId(), created.getUsername(), created.getId());
    log.info("User '{}' registered for tenant {}", created.getUsername(), created.getTenantId());
    return BaseResponse.success("User registered", tokens);
  }

  @Transactional
  @Override
  public BaseResponse<AuthResponse> login(AuthRequest req) {
    UUID tenantId = req.getTenantId();
    log.info("User '{}' attempting login for tenant {}", req.getIdentifier(), tenantId);
    // identifier can be username or email
    User user = userRepository.findByTenantIdAndUsername(tenantId, req.getIdentifier())
        .or(() -> userRepository.findByTenantIdAndEmail(tenantId, req.getIdentifier()))
        .orElseThrow(() -> new NoSuchElementException("Invalid credentials"));

    if (!user.isEnabled() || user.isLocked()) {
      log.warn("Login denied for user '{}' in tenant {}: disabled or locked", user.getUsername(), tenantId);
      throw new IllegalStateException("Account disabled or locked");
    }
    if (!PasswordHasher.matchesBcrypt(req.getPassword(), user.getPasswordHash())) {
      log.warn("Invalid credentials for user '{}' in tenant {}", req.getIdentifier(), tenantId);
      throw new NoSuchElementException("Invalid credentials");
    }
    var tokens = issueTokens(user.getTenantId(), user.getUsername(), user.getId());
    log.info("User '{}' logged in for tenant {}", user.getUsername(), tenantId);
    return BaseResponse.success("Login successful", tokens);
  }

  @Transactional
  @Override
  public BaseResponse<AuthResponse> refresh(RefreshTokenRequest req) {
    var user = refreshTokenService.validateAndGetUser(req.getRefreshToken());
    log.info("Refreshing token for user '{}' in tenant {}", user.getUsername(), user.getTenantId());
    var tokens = issueTokens(user.getTenantId(), user.getUsername(), user.getId());
    return BaseResponse.success("Token refreshed", tokens);
  }

  @Transactional
  @Override
  public BaseResponse<Void> logout(String refreshToken) {
    log.info("Revoking refresh token during logout");
    refreshTokenService.revoke(refreshToken);
    return BaseResponse.success("Logged out", null);
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
