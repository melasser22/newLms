package com.ejada.sec.service.impl;

import com.ejada.sec.domain.User;
import com.ejada.sec.dto.*;
import com.ejada.sec.mapper.ReferenceResolver;
import com.ejada.sec.mapper.UserMapper;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final ReferenceResolver resolver;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  @Override
  public UserDto create(CreateUserRequest req) {
    User user = userMapper.toEntity(req);
    user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
    user = userRepository.save(user);
    // attach roles by codes (if any)
    userMapper.setRolesByCodes(user, req.getRoles(), req.getTenantId(), resolver);
    user = userRepository.save(user);
    return userMapper.toDto(user, resolver);
  }

  @Transactional
  @Override
  public UserDto update(Long userId, UpdateUserRequest req) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    userMapper.updateEntity(user, req);
    user = userRepository.save(user);
    // roles changes typically go via GrantService, but if you pass role codes in Update, you can:
    // userMapper.setRolesByCodes(user, req.getRoles(), user.getTenantId(), resolver);
    return userMapper.toDto(user, resolver);
  }

  @Transactional
  @Override
  public void delete(Long userId) {
    if (!userRepository.existsById(userId)) return;
    userRepository.deleteById(userId);
  }

  @Override
  public UserDto get(Long userId) {
    return userRepository.findById(userId)
        .map(u -> userMapper.toDto(u, resolver))
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
  }

  @Override
  public List<UserDto> listByTenant(UUID tenantId) {
    return userMapper.toDto(userRepository.findAllByTenantId(tenantId), resolver);
  }

  @Transactional @Override public void enable(Long userId)  { setEnabled(userId, true); }
  @Transactional @Override public void disable(Long userId) { setEnabled(userId, false); }
  @Transactional @Override public void lock(Long userId)    { setLocked(userId, true); }
  @Transactional @Override public void unlock(Long userId)  { setLocked(userId, false); }

  private void setEnabled(Long userId, boolean flag) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    user.setEnabled(flag);
    userRepository.save(user);
  }
  private void setLocked(Long userId, boolean flag) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    user.setLocked(flag);
    userRepository.save(user);
  }
}
