package com.ejada.sec.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.dto.*;
import java.util.List;

public interface UserService {
  BaseResponse<UserDto> create(CreateUserRequest req);
  BaseResponse<UserDto> update(Long userId, UpdateUserRequest req);
  BaseResponse<Void>   delete(Long userId);
  BaseResponse<UserDto> get(Long userId);
  BaseResponse<List<UserDto>> listByTenant();

  // role membership (full replace helpers are in GrantService below)
  BaseResponse<Void> enable(Long userId);
  BaseResponse<Void> disable(Long userId);
  BaseResponse<Void> lock(Long userId);
  BaseResponse<Void> unlock(Long userId);
}
