package com.ejada.sec.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.redis.starter.support.RedisCacheHelper;
import com.ejada.sec.domain.Privilege;
import com.ejada.sec.dto.*;
import com.ejada.sec.mapper.PrivilegeMapper;
import com.ejada.sec.repository.PrivilegeRepository;
import com.ejada.sec.service.PrivilegeService;
import com.ejada.sec.util.TenantContextResolver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrivilegeServiceImpl implements PrivilegeService {

  private final PrivilegeRepository repository;
  private final PrivilegeMapper mapper;
  private final RedisCacheHelper cache;

  private static final String PRIV_KEY_PREFIX = "priv:";
  private static final String PRIV_LIST_KEY_PREFIX = "privs:tenant:";

  @Transactional
  @Override
  public BaseResponse<PrivilegeDto> create(CreatePrivilegeRequest req) {
    Privilege p = repository.save(mapper.toEntity(req));
    PrivilegeDto dto = mapper.toDto(p);
    cachePrivilege(dto);
    return BaseResponse.success("Privilege created", dto);
  }

  @Transactional
  @Override
  public BaseResponse<PrivilegeDto> update(Long id, UpdatePrivilegeRequest req) {
    Privilege p =
        repository
            .findByIdSecure(id)
            .orElseThrow(() -> new NoSuchElementException("Privilege not found: " + id));
    mapper.updateEntity(p, req);
    p = repository.save(p);
    PrivilegeDto dto = mapper.toDto(p);
    cachePrivilege(dto);
    return BaseResponse.success("Privilege updated", dto);
  }

  @Transactional
  @Override
  public BaseResponse<Void> delete(Long id) {
    return repository
        .findByIdSecure(id)
        .map(
            privilege -> {
              UUID tenantId = privilege.getTenantId();
              repository.delete(privilege);
              cache.delete(privKeySuffix(privilege.getId()));
              if (tenantId != null) {
                cache.delete(privListKeySuffix(tenantId));
              }
              return BaseResponse.<Void>success("Privilege deleted", null);
            })
        .orElseGet(() -> BaseResponse.<Void>success("Privilege deleted", null));
  }

  @Override
  public BaseResponse<PrivilegeDto> get(Long id) {
    return cache.<PrivilegeDto>get(privKeySuffix(id))
        .map(dto -> BaseResponse.success("Privilege fetched", dto))
        .orElseGet(
            () ->
                repository
                    .findByIdSecure(id)
                    .map(mapper::toDto)
                    .map(
                        dto -> {
                          cache.set(privKeySuffix(dto.getId()), dto);
                          return BaseResponse.success("Privilege fetched", dto);
                        })
                    .orElseThrow(() -> new NoSuchElementException("Privilege not found: " + id)));
  }

  @Override
  public BaseResponse<List<PrivilegeDto>> listByTenant() {
    UUID tenantId = TenantContextResolver.requireTenantId();
    return cache.<List<PrivilegeDto>>get(privListKeySuffix(tenantId))
        .map(list -> BaseResponse.success("Privileges listed", list))
        .orElseGet(
            () -> {
              List<PrivilegeDto> list = mapper.toDto(repository.findAllSecure());
              cache.set(privListKeySuffix(tenantId), list);
              return BaseResponse.success("Privileges listed", list);
            });
  }

  private void cachePrivilege(PrivilegeDto dto) {
    cache.set(privKeySuffix(dto.getId()), dto);
    if (dto.getTenantId() != null) {
      cache.delete(privListKeySuffix(dto.getTenantId()));
    }
  }

  private String privKeySuffix(Long id) {
    return PRIV_KEY_PREFIX + id;
  }

  private String privListKeySuffix(UUID tenantId) {
    return PRIV_LIST_KEY_PREFIX + tenantId;
  }
}
