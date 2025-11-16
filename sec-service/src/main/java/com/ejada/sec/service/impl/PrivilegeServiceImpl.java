package com.ejada.sec.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.domain.Privilege;
import com.ejada.sec.dto.*;
import com.ejada.sec.mapper.PrivilegeMapper;
import com.ejada.sec.context.TenantContextProvider;
import com.ejada.sec.repository.PrivilegeRepository;
import com.ejada.sec.service.PrivilegeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.ejada.redis.starter.config.KeyPrefixStrategy;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrivilegeServiceImpl implements PrivilegeService {

  private final PrivilegeRepository repository;
  private final PrivilegeMapper mapper;
  private final RedisTemplate<String, Object> redisTemplate;
  private final KeyPrefixStrategy keyPrefixStrategy;
  private final TenantContextProvider tenantContextProvider;

  private static final String PRIV_KEY_PREFIX = "priv:";
  private static final String PRIV_LIST_KEY_PREFIX = "privs:tenant:";

  @Transactional
  @Override
  public BaseResponse<PrivilegeDto> create(CreatePrivilegeRequest req) {
    Privilege p = repository.save(mapper.toEntity(req));
    PrivilegeDto dto = mapper.toDto(p);
    redisTemplate.opsForValue().set(privKey(p.getId()), dto);
    redisTemplate.delete(privListKey(p.getTenantId()));
    return BaseResponse.success("Privilege created", dto);
  }

  @Transactional
  @Override
  public BaseResponse<PrivilegeDto> update(Long id, UpdatePrivilegeRequest req) {
    Privilege p =
        repository
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("Privilege not found: " + id));
    mapper.updateEntity(p, req);
    p = repository.save(p);
    PrivilegeDto dto = mapper.toDto(p);
    redisTemplate.opsForValue().set(privKey(p.getId()), dto);
    redisTemplate.delete(privListKey(p.getTenantId()));
    return BaseResponse.success("Privilege updated", dto);
  }

  @Transactional
  @Override
  public BaseResponse<Void> delete(Long id) {
    if (repository.existsById(id)) {
      repository.deleteById(id);
      redisTemplate.delete(privKey(id));
    }
    // invalidate all privilege lists since tenant is unknown
    String prefix = keyPrefixStrategy.resolvePrefix() + PRIV_LIST_KEY_PREFIX;
    redisTemplate.keys(prefix + "*").forEach(redisTemplate::delete);
    return BaseResponse.success("Privilege deleted", null);
  }

  @Override
  public BaseResponse<PrivilegeDto> get(Long id) {
    String key = privKey(id);
    PrivilegeDto cached = (PrivilegeDto) redisTemplate.opsForValue().get(key);
    if (cached != null) {
      return BaseResponse.success("Privilege fetched", cached);
    }
    return repository.findById(id)
        .map(mapper::toDto)
        .map(
            dto -> {
              redisTemplate.opsForValue().set(key, dto);
              return BaseResponse.success("Privilege fetched", dto);
            })
        .orElseThrow(() -> new NoSuchElementException("Privilege not found: " + id));
  }

  @Override
  public BaseResponse<List<PrivilegeDto>> listByTenant() {
    UUID tenantId = tenantContextProvider.requireTenantId();
    String key = privListKey(tenantId);
    @SuppressWarnings("unchecked")
    List<PrivilegeDto> cached = (List<PrivilegeDto>) redisTemplate.opsForValue().get(key);
    if (cached != null) {
      return BaseResponse.success("Privileges listed", cached);
    }
    List<PrivilegeDto> list = mapper.toDto(repository.findAllByTenantId(tenantId));
    redisTemplate.opsForValue().set(key, list);
    return BaseResponse.success("Privileges listed", list);
  }

  private String privKey(Long id) {
    return keyPrefixStrategy.resolvePrefix() + PRIV_KEY_PREFIX + id;
  }

  private String privListKey(UUID tenantId) {
    return keyPrefixStrategy.resolvePrefix() + PRIV_LIST_KEY_PREFIX + tenantId;
  }
}
