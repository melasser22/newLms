package com.ejada.sec.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.domain.Role;
import com.ejada.sec.dto.*;
import com.ejada.sec.mapper.ReferenceResolver;
import com.ejada.sec.mapper.RoleMapper;
import com.ejada.sec.repository.RoleRepository;
import com.ejada.sec.service.RoleService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import com.ejada.common.context.ContextManager;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

  private final RoleRepository roleRepository;
  private final RoleMapper roleMapper;
  private final ReferenceResolver resolver;

  @Transactional
  @Override
  public BaseResponse<RoleDto> create(CreateRoleRequest req) {
    Role role = roleMapper.toEntity(req);
    role = roleRepository.save(role);
    return BaseResponse.success("Role created", roleMapper.toDto(role, resolver));
  }

  @Transactional
  @Override
  public BaseResponse<RoleDto> update(Long roleId, UpdateRoleRequest req) {
    Role role = roleRepository.findById(roleId)
        .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
    roleMapper.updateEntity(role, req);
    role = roleRepository.save(role);
    return BaseResponse.success("Role updated", roleMapper.toDto(role, resolver));
  }

  @Transactional
  @Override
  public BaseResponse<Void> delete(Long roleId) {
    if (roleRepository.existsById(roleId)) {
      roleRepository.deleteById(roleId);
    }
    return BaseResponse.success("Role deleted", null);
  }

  @Override
  public BaseResponse<RoleDto> get(Long roleId) {
    return roleRepository.findById(roleId)
        .map(r -> roleMapper.toDto(r, resolver))
        .map(dto -> BaseResponse.success("Role fetched", dto))
        .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
  }

  @Override
  public BaseResponse<List<RoleDto>> listByTenant() {
    UUID tenantId = UUID.fromString(ContextManager.Tenant.get());
    return BaseResponse.success("Roles listed",
        roleMapper.toDto(roleRepository.findAllByTenantId(tenantId), resolver));
  }
}
