package com.ejada.sec.service.impl;

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

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

  private final RoleRepository roleRepository;
  private final RoleMapper roleMapper;
  private final ReferenceResolver resolver;

  @Transactional
  @Override
  public RoleDto create(CreateRoleRequest req) {
    Role role = roleMapper.toEntity(req);
    role = roleRepository.save(role);
    return roleMapper.toDto(role, resolver);
  }

  @Transactional
  @Override
  public RoleDto update(Long roleId, UpdateRoleRequest req) {
    Role role = roleRepository.findById(roleId)
        .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
    roleMapper.updateEntity(role, req);
    role = roleRepository.save(role);
    return roleMapper.toDto(role, resolver);
  }

  @Transactional
  @Override
  public void delete(Long roleId) {
    if (!roleRepository.existsById(roleId)) return;
    roleRepository.deleteById(roleId);
  }

  @Override
  public RoleDto get(Long roleId) {
    return roleRepository.findById(roleId)
        .map(r -> roleMapper.toDto(r, resolver))
        .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
  }

  @Override
  public List<RoleDto> listByTenant(UUID tenantId) {
    return roleMapper.toDto(roleRepository.findAllByTenantId(tenantId), resolver);
  }
}
