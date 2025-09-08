package com.ejada.sec.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.domain.Privilege;
import com.ejada.sec.dto.*;
import com.ejada.sec.mapper.PrivilegeMapper;
import com.ejada.sec.repository.PrivilegeRepository;
import com.ejada.sec.service.PrivilegeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import com.ejada.common.context.ContextManager;

@Service
@RequiredArgsConstructor
public class PrivilegeServiceImpl implements PrivilegeService {

  private final PrivilegeRepository repository;
  private final PrivilegeMapper mapper;

  @Transactional
  @Override
  public BaseResponse<PrivilegeDto> create(CreatePrivilegeRequest req) {
    Privilege p = repository.save(mapper.toEntity(req));
    return BaseResponse.success("Privilege created", mapper.toDto(p));
  }

  @Transactional
  @Override
  public BaseResponse<PrivilegeDto> update(Long id, UpdatePrivilegeRequest req) {
    Privilege p = repository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Privilege not found: " + id));
    mapper.updateEntity(p, req);
    return BaseResponse.success("Privilege updated", mapper.toDto(repository.save(p)));
  }

  @Transactional
  @Override
  public BaseResponse<Void> delete(Long id) {
    if (repository.existsById(id)) {
      repository.deleteById(id);
    }
    return BaseResponse.success("Privilege deleted", null);
  }

  @Override
  public BaseResponse<PrivilegeDto> get(Long id) {
    return repository.findById(id)
        .map(mapper::toDto)
        .map(dto -> BaseResponse.success("Privilege fetched", dto))
        .orElseThrow(() -> new NoSuchElementException("Privilege not found: " + id));
  }

  @Override
  public BaseResponse<List<PrivilegeDto>> listByTenant() {
    UUID tenantId = UUID.fromString(ContextManager.Tenant.get());
    return BaseResponse.success("Privileges listed",
        mapper.toDto(repository.findAllByTenantId(tenantId)));
  }
}
