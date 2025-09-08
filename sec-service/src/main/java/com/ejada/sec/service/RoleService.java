package com.ejada.sec.service;

import com.ejada.sec.dto.*;
import java.util.List;
import java.util.UUID;

public interface RoleService {
  RoleDto create(CreateRoleRequest req);
  RoleDto update(Long roleId, UpdateRoleRequest req);
  void    delete(Long roleId);
  RoleDto get(Long roleId);
  List<RoleDto> listByTenant(UUID tenantId);
}
