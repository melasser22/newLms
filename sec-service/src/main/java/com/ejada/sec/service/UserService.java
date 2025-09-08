package com.ejada.sec.service;

import com.ejada.sec.dto.*;
import java.util.List;
import java.util.UUID;

public interface UserService {
  UserDto create(CreateUserRequest req);
  UserDto update(Long userId, UpdateUserRequest req);
  void   delete(Long userId);
  UserDto get(Long userId);
  List<UserDto> listByTenant(UUID tenantId);

  // role membership (full replace helpers are in GrantService below)
  void enable(Long userId);
  void disable(Long userId);
  void lock(Long userId);
  void unlock(Long userId);
}
