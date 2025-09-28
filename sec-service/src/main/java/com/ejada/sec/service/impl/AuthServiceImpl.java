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

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

  private static final String CODE_USER_CREATE_FAILED = "ERR-USER-CREATE";
  private static final String CODE_AUTH_INVALID = "ERR-AUTH-INVALID";
  private static final String CODE_AUTH_LOCKED = "ERR-AUTH-LOCKED";

  private final UserRepository userRepository;
  private final UserService userService;
  private final RefreshTokenService refreshTokenService;
  private final TokenIssuer tokenIssuer; // see below

  @Transactional
  @Override
  public BaseResponse<AuthResponse> register(RegisterRequest req) {
    log.info("Registering user '{}' for tenant {}", req.getUsername(), req.getTenantId());
    var creationResponse = userService.create(
        CreateUserRequest.builder()
            .tenantId(req.getTenantId())
            .username(req.getUsername())
            .email(req.getEmail())
            .password(req.getPassword())
            .roles(req.getRoles())
            .build()
    );

    if (!creationResponse.isSuccess() || creationResponse.getData() == null) {
      log.warn(
          "User '{}' registration failed for tenant {} with code {}",
          req.getUsername(),
          req.getTenantId(),
          creationResponse.getCode());
      return creationResponse.map(data -> null);
    }

    var created = creationResponse.getData();
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
    var userOpt = userRepository.findByTenantIdAndUsername(tenantId, req.getIdentifier())
        .or(() -> userRepository.findByTenantIdAndEmail(tenantId, req.getIdentifier()));

    if (userOpt.isEmpty()) {
      log.warn("Invalid credentials for identifier '{}' in tenant {}", req.getIdentifier(), tenantId);
      return BaseResponse.error(CODE_AUTH_INVALID, "Invalid credentials");
    }

    User user = userOpt.get();

    if (!user.isEnabled() || user.isLocked()) {
      log.warn("Login denied for user '{}' in tenant {}: disabled or locked", user.getUsername(), tenantId);
      return BaseResponse.error(CODE_AUTH_LOCKED, "Account disabled or locked");
    }
    if (!PasswordHasher.matchesBcrypt(req.getPassword(), user.getPasswordHash())) {
      log.warn("Invalid credentials for user '{}' in tenant {}", req.getIdentifier(), tenantId);
      return BaseResponse.error(CODE_AUTH_INVALID, "Invalid credentials");
    }
    var tokens = issueTokens(user.getTenantId(), user.getUsername(), user.getId(), true, null);
    log.info("User '{}' logged in for tenant {}", user.getUsername(), tenantId);
    return BaseResponse.success("Login successful", tokens);
  }

  @Transactional
  @Override
  public BaseResponse<AuthResponse> refresh(RefreshTokenRequest req) {
    var user = refreshTokenService.validateAndGetUser(req.getRefreshToken());
    log.info("Refreshing token for user '{}' in tenant {}", user.getUsername(), user.getTenantId());
    refreshTokenService.revoke(req.getRefreshToken());
    var tokens = issueTokens(user.getTenantId(), user.getUsername(), user.getId(), false, req.getRefreshToken());
    return BaseResponse.success("Token refreshed", tokens);
  }

  @Transactional
  @Override
  public BaseResponse<Void> logout(String refreshToken) {
    log.info("Revoking refresh token during logout");
    refreshTokenService.revoke(refreshToken);
    return BaseResponse.success("Logged out", null);
  }

  private AuthResponse issueTokens(UUID tenantId, String username, Long userId, boolean revokeExistingSessions, String rotatedFrom) {
    String access = tokenIssuer.issueAccessToken(tenantId, userId, username);
    String refresh = refreshTokenService.issue(userId, revokeExistingSessions, rotatedFrom);
    return AuthResponse.builder()
        .accessToken(access)
        .refreshToken(refresh)
        .tokenType("Bearer")
        .expiresInSeconds(tokenIssuer.getAccessTokenTtlSeconds())
        .build();
  }
}
