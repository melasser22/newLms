package com.ejada.sec.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.domain.User;
import com.ejada.sec.domain.UserRole;
import com.ejada.sec.dto.*;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.service.AuthService;
import com.ejada.sec.service.RefreshTokenService;
import com.ejada.sec.service.TokenIssuer;
import com.ejada.sec.service.UserService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final UserService userService;
  private final RefreshTokenService refreshTokenService;
  private final PasswordEncoder passwordEncoder;
  private final TokenIssuer tokenIssuer; // see below

  @Value("${security.password.expiry-days:90}")
  private long passwordExpiryDays;

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
    List<String> roles = created.getRoles() == null
        ? List.of()
        : List.copyOf(created.getRoles());
    log.info("User '{}' registered for tenant {}", created.getUsername(), created.getTenantId());
    return BaseResponse.success(
        "User registered. First login required",
        buildFirstLoginResponse(
            roles,
            "Account created successfully. Please login and complete first login to activate access."));
  }

  @Transactional
  @Override
  public BaseResponse<AuthResponse> login(AuthRequest req) {
    User user = findByIdentifier(req.getIdentifier());

    if (!user.isEnabled() || user.isLocked()) {
      log.warn("Login denied for user '{}' in tenant {}: disabled or locked", user.getUsername(), user.getTenantId());
      throw new IllegalStateException("Account disabled or locked");
    }
    if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
      log.warn("Invalid credentials for user '{}' in tenant {}", req.getIdentifier(), user.getTenantId());
      throw new NoSuchElementException("Invalid credentials");
    }
    var roles = resolveRoleCodes(user);
    if (!user.isFirstLoginCompleted()) {
      log.info("First login required for user '{}' in tenant {}", user.getUsername(), user.getTenantId());
      return BaseResponse.success(
          "First login required",
          buildFirstLoginResponse(
              roles,
              "First login detected. Please change your password to activate your account."));
    }
    user.setLastLoginAt(Instant.now());
    userRepository.save(user);
    var tokens = issueTokens(
        user.getTenantId(), user.getUsername(), user.getId(), roles, "Login successful");
    log.info("User '{}' logged in for tenant {}", user.getUsername(), user.getTenantId());
    return BaseResponse.success("Login successful", tokens);
  }

  @Transactional
  @Override
  public BaseResponse<AuthResponse> refresh(RefreshTokenRequest req) {
    var user = refreshTokenService.validateAndGetUser(req.getRefreshToken());
    if (!user.isFirstLoginCompleted()) {
      throw new IllegalStateException("First login must be completed before refreshing tokens");
    }
    log.info("Refreshing token for user '{}' in tenant {}", user.getUsername(), user.getTenantId());
    var tokens = issueTokens(
        user.getTenantId(), user.getUsername(), user.getId(), resolveRoleCodes(user), "Token refreshed");
    return BaseResponse.success("Token refreshed", tokens);
  }

  @Transactional
  @Override
  public BaseResponse<Void> logout(String refreshToken) {
    log.info("Revoking refresh token during logout");
    refreshTokenService.revoke(refreshToken);
    return BaseResponse.success("Logged out", null);
  }

  @Transactional
  @Override
  public BaseResponse<Void> completeFirstLogin(CompleteFirstLoginRequest req) {
    User user = findByIdentifier(req.getIdentifier());
    if (user.isFirstLoginCompleted()) {
      throw new IllegalStateException("First login has already been completed");
    }
    if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Current password is incorrect");
    }
    if (req.getNewPassword().equals(req.getCurrentPassword())) {
      throw new IllegalArgumentException("New password must be different from current password");
    }
    if (!req.getNewPassword().equals(req.getConfirmPassword())) {
      throw new IllegalArgumentException("New password and confirmation do not match");
    }

    user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
    user.setFirstLoginCompleted(true);
    user.setPasswordChangedAt(Instant.now());
    user.setPasswordExpiresAt(calculatePasswordExpiry());
    userRepository.save(user);

    log.info("First login completed for user '{}' in tenant {}", user.getUsername(), user.getTenantId());
    return BaseResponse.success(
        "First login completed successfully. Please login again with your new password.",
        null);
  }

  private User findByIdentifier(String identifier) {
    return userRepository.findByUsername(identifier)
        .or(() -> userRepository.findByEmail(identifier))
        .orElseThrow(() -> new NoSuchElementException("Invalid credentials"));
  }

  private List<String> resolveRoleCodes(User user) {
    if (user.getRoles() == null || user.getRoles().isEmpty()) {
      return List.of();
    }
    return user.getRoles().stream()
        .map(UserRole::getRole)
        .filter(java.util.Objects::nonNull)
        .map(role -> role.getCode())
        .filter(code -> code != null && !code.isBlank())
        .sorted(Comparator.naturalOrder())
        .toList();
  }

  private AuthResponse buildFirstLoginResponse(List<String> roles, String message) {
    List<String> normalizedRoles = roles == null ? List.of() : List.copyOf(roles);
    return AuthResponse.builder()
        .roles(normalizedRoles)
        .role(normalizedRoles.isEmpty() ? null : normalizedRoles.get(0))
        .permissions(List.of("CHANGE_PASSWORD"))
        .requiresPasswordChange(true)
        .passwordExpired(false)
        .message(message)
        .build();
  }

  private AuthResponse issueTokens(
      UUID tenantId,
      String username,
      Long userId,
      List<String> roles,
      String message) {
    List<String> normalizedRoles = roles == null ? List.of() : List.copyOf(roles);
    String access = tokenIssuer.issueAccessToken(tenantId, userId, username, normalizedRoles);
    String refresh = refreshTokenService.issue(userId);
    return AuthResponse.builder()
        .accessToken(access)
        .refreshToken(refresh)
        .tokenType("Bearer")
        .expiresInSeconds(tokenIssuer.getAccessTokenTtlSeconds())
        .role(normalizedRoles.isEmpty() ? null : normalizedRoles.get(0))
        .roles(normalizedRoles)
        .permissions(List.of())
        .requiresPasswordChange(false)
        .passwordExpired(false)
        .message(message)
        .build();
  }

  private Instant calculatePasswordExpiry() {
    if (passwordExpiryDays <= 0) {
      return null;
    }
    return Instant.now().plus(passwordExpiryDays, ChronoUnit.DAYS);
  }
}
