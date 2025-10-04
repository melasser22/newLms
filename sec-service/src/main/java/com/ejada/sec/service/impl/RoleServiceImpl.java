package com.ejada.sec.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.redis.starter.support.RedisCacheHelper;
import com.ejada.sec.domain.Role;
import com.ejada.sec.dto.*;
import com.ejada.sec.mapper.ReferenceResolver;
import com.ejada.sec.mapper.RoleMapper;
import com.ejada.sec.repository.RoleRepository;
import com.ejada.sec.service.RoleService;
import com.ejada.sec.util.TenantContextResolver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

  private final RoleRepository roleRepository;
  private final RoleMapper roleMapper;
  private final ReferenceResolver resolver;
  private final RedisCacheHelper cache;

  private static final String ROLE_KEY_PREFIX = "role:";
  private static final String ROLE_LIST_KEY_PREFIX = "roles:tenant:";

  @Transactional
  @Override
  public BaseResponse<RoleDto> create(CreateRoleRequest req) {
      log.info("Creating role '{}' for tenant {}", req.getName(), req.getTenantId());
      Role role = roleMapper.toEntity(req);
      role = roleRepository.save(role);
      RoleDto dto = roleMapper.toDto(role, resolver);
      cacheRole(dto);
      log.info("Role '{}' created with id {}", role.getName(), role.getId());
      return BaseResponse.success("Role created", dto);
    }

  @Transactional
  @Override
  public BaseResponse<RoleDto> update(Long roleId, UpdateRoleRequest req) {
      log.info("Updating role {}", roleId);
      Role role = roleRepository.findByIdSecure(roleId)
          .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
      roleMapper.updateEntity(role, req);
      role = roleRepository.save(role);
      RoleDto dto = roleMapper.toDto(role, resolver);
      cacheRole(dto);
      log.info("Role {} updated", roleId);
      return BaseResponse.success("Role updated", dto);
    }

  @Transactional
  @Override
  public BaseResponse<Void> delete(Long roleId) {
      log.info("Deleting role {}", roleId);
      roleRepository
          .findByIdSecure(roleId)
          .ifPresent(role -> {
            roleRepository.delete(role);
            cache.delete(roleKeySuffix(role.getId()));
            UUID tenantId = role.getTenantId();
            if (tenantId != null) {
              cache.delete(roleListKeySuffix(tenantId));
            }
          });
      log.info("Role {} deleted", roleId);
      return BaseResponse.success("Role deleted", null);
    }

  @Override
  public BaseResponse<RoleDto> get(Long roleId) {
      log.debug("Fetching role {}", roleId);
      return cache.<RoleDto>get(roleKeySuffix(roleId))
          .map(
              dto -> {
                log.debug("Role {} served from cache", roleId);
                return BaseResponse.success("Role fetched", dto);
              })
          .orElseGet(
              () ->
                  roleRepository.findByIdSecure(roleId)
                      .map(r -> roleMapper.toDto(r, resolver))
                      .map(
                          dto -> {
                            cache.set(roleKeySuffix(dto.getId()), dto);
                            return BaseResponse.success("Role fetched", dto);
                          })
                      .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId)));
    }

  @Override
  public BaseResponse<List<RoleDto>> listByTenant() {
      UUID tenantId = TenantContextResolver.requireTenantId();
      log.debug("Listing roles for tenant {}", tenantId);
      return cache.<List<RoleDto>>get(roleListKeySuffix(tenantId))
          .map(
              list -> {
                log.debug("Returning cached role list for tenant {}", tenantId);
                return BaseResponse.success("Roles listed", list);
              })
          .orElseGet(
              () -> {
                List<RoleDto> list =
                    roleMapper.toDto(roleRepository.findAllSecure(), resolver);
                cache.set(roleListKeySuffix(tenantId), list);
                return BaseResponse.success("Roles listed", list);
              });
    }

  private void cacheRole(RoleDto dto) {
    cache.set(roleKeySuffix(dto.getId()), dto);
    if (dto.getTenantId() != null) {
      cache.delete(roleListKeySuffix(dto.getTenantId()));
    }
  }

  private String roleKeySuffix(Long id) {
    return ROLE_KEY_PREFIX + id;
  }

  private String roleListKeySuffix(UUID tenantId) {
    return ROLE_LIST_KEY_PREFIX + tenantId;
  }
}
