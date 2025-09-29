package com.ejada.sec.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.crypto.password.PasswordHasher;
import com.ejada.sec.domain.User;
import com.ejada.sec.dto.*;
import com.ejada.sec.mapper.ReferenceResolver;
import com.ejada.sec.mapper.UserMapper;
import com.ejada.sec.repository.PasswordResetTokenRepository;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.service.UserService;
import com.ejada.sec.service.RefreshTokenService;
import com.ejada.sec.util.TenantContextResolver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final ReferenceResolver resolver;
  private final RefreshTokenService refreshTokenService;
  private final PasswordResetTokenRepository passwordResetTokenRepository;

  @Transactional
  @Override
  public BaseResponse<UserDto> create(CreateUserRequest req) {
    User user = userMapper.toEntity(req);
    user.setPasswordHash(PasswordHasher.bcrypt(req.getPassword()));
    user = userRepository.save(user);
    // attach roles by codes (if any)
    userMapper.setRolesByCodes(user, req.getRoles(), req.getTenantId(), resolver);
    user = userRepository.save(user);
    return BaseResponse.success("User created", userMapper.toDto(user, resolver));
  }

  @Transactional
  @Override
  public BaseResponse<UserDto> update(Long userId, UpdateUserRequest req) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    userMapper.updateEntity(user, req);
    user = userRepository.save(user);
    // roles changes typically go via GrantService, but if you pass role codes in Update, you can:
    // userMapper.setRolesByCodes(user, req.getRoles(), user.getTenantId(), resolver);
    return BaseResponse.success("User updated", userMapper.toDto(user, resolver));
  }

  @Transactional
  @Override
  public BaseResponse<Void> delete(Long userId) {
    userRepository.findById(userId).ifPresent(user -> {
      revokeUserCredentials(userId);
      passwordResetTokenRepository.deleteByUserId(userId);
      userRepository.delete(user);
    });
    return BaseResponse.success("User deleted", null);
  }

  @Override
  public BaseResponse<UserDto> get(Long userId) {
    return userRepository.findById(userId)
        .map(u -> userMapper.toDto(u, resolver))
        .map(dto -> BaseResponse.success("User fetched", dto))
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
  }

  @Override
  public BaseResponse<List<UserDto>> listByTenant() {
    UUID tenantId = TenantContextResolver.requireTenantId();
    return BaseResponse.success(
        "Users listed", userMapper.toDto(userRepository.findAllByTenantId(tenantId), resolver));
  }

  @Transactional
  @Override
  public BaseResponse<Void> enable(Long userId) {
    return setEnabled(userId, true, "User enabled");
  }

  @Transactional
  @Override
  public BaseResponse<Void> disable(Long userId) {
    return setEnabled(userId, false, "User disabled");
  }

  @Transactional
  @Override
  public BaseResponse<Void> lock(Long userId) {
    return setLocked(userId, true, "User locked");
  }

  @Transactional
  @Override
  public BaseResponse<Void> unlock(Long userId) {
    return setLocked(userId, false, "User unlocked");
  }

  private BaseResponse<Void> setEnabled(Long userId, boolean flag, String message) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    user.setEnabled(flag);
    if (!flag) {
      revokeUserCredentials(userId);
    }
    userRepository.save(user);
    return BaseResponse.success(message, null);
  }
  private BaseResponse<Void> setLocked(Long userId, boolean flag, String message) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    user.setLocked(flag);
    if (flag) {
      revokeUserCredentials(userId);
    }
    userRepository.save(user);
    return BaseResponse.success(message, null);
  }

  private void revokeUserCredentials(Long userId) {
    refreshTokenService.revokeAllForUser(userId);
    passwordResetTokenRepository.invalidateActiveTokens(userId, Instant.now());
  }
}
