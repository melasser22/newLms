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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.ejada.redis.starter.config.KeyPrefixStrategy;

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
  private final RedisTemplate<String, Object> redisTemplate;
  private final KeyPrefixStrategy keyPrefixStrategy;

  private static final String ROLE_KEY_PREFIX = "role:";
  private static final String ROLE_LIST_KEY_PREFIX = "roles:tenant:";

  @Transactional
  @Override
  public BaseResponse<RoleDto> create(CreateRoleRequest req) {
    Role role = roleMapper.toEntity(req);
    role = roleRepository.save(role);
    RoleDto dto = roleMapper.toDto(role, resolver);
    redisTemplate.opsForValue().set(roleKey(role.getId()), dto);
    redisTemplate.delete(roleListKey(role.getTenantId()));
    return BaseResponse.success("Role created", dto);
  }

  @Transactional
  @Override
  public BaseResponse<RoleDto> update(Long roleId, UpdateRoleRequest req) {
    Role role = roleRepository.findById(roleId)
        .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
    roleMapper.updateEntity(role, req);
    role = roleRepository.save(role);
    RoleDto dto = roleMapper.toDto(role, resolver);
    redisTemplate.opsForValue().set(roleKey(role.getId()), dto);
    redisTemplate.delete(roleListKey(role.getTenantId()));
    return BaseResponse.success("Role updated", dto);
  }

  @Transactional
  @Override
  public BaseResponse<Void> delete(Long roleId) {
    if (roleRepository.existsById(roleId)) {
      roleRepository.deleteById(roleId);
      redisTemplate.delete(roleKey(roleId));
    }
    // invalidate tenant role list cache - tenant cannot be determined from id, so clear all
    // role lists
    String prefix = keyPrefixStrategy.resolvePrefix() + ROLE_LIST_KEY_PREFIX;
    redisTemplate.keys(prefix + "*").forEach(redisTemplate::delete);
    return BaseResponse.success("Role deleted", null);
  }

  @Override
  public BaseResponse<RoleDto> get(Long roleId) {
    String key = roleKey(roleId);
    RoleDto cached = (RoleDto) redisTemplate.opsForValue().get(key);
    if (cached != null) {
      return BaseResponse.success("Role fetched", cached);
    }
    return roleRepository.findById(roleId)
        .map(r -> roleMapper.toDto(r, resolver))
        .map(
            dto -> {
              redisTemplate.opsForValue().set(key, dto);
              return BaseResponse.success("Role fetched", dto);
            })
        .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
  }

  @Override
  public BaseResponse<List<RoleDto>> listByTenant() {
    UUID tenantId = UUID.fromString(ContextManager.Tenant.get());
    String key = roleListKey(tenantId);
    @SuppressWarnings("unchecked")
    List<RoleDto> cached = (List<RoleDto>) redisTemplate.opsForValue().get(key);
    if (cached != null) {
      return BaseResponse.success("Roles listed", cached);
    }
    List<RoleDto> list =
        roleMapper.toDto(roleRepository.findAllByTenantId(tenantId), resolver);
    redisTemplate.opsForValue().set(key, list);
    return BaseResponse.success("Roles listed", list);
  }

  private String roleKey(Long id) {
    return keyPrefixStrategy.resolvePrefix() + ROLE_KEY_PREFIX + id;
  }

  private String roleListKey(UUID tenantId) {
    return keyPrefixStrategy.resolvePrefix() + ROLE_LIST_KEY_PREFIX + tenantId;
  }
}
