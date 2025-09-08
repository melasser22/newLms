package com.ejada.sec.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.dto.*;
import java.util.List;

public interface RoleService {
  BaseResponse<RoleDto> create(CreateRoleRequest req);
  BaseResponse<RoleDto> update(Long roleId, UpdateRoleRequest req);
  BaseResponse<Void>    delete(Long roleId);
  BaseResponse<RoleDto> get(Long roleId);
  BaseResponse<List<RoleDto>> listByTenant();
}
