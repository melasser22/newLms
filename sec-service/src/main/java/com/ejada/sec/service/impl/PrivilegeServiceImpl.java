package com.ejada.sec.service.impl;

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

@Service
@RequiredArgsConstructor
public class PrivilegeServiceImpl implements PrivilegeService {

  private final PrivilegeRepository repository;
  private final PrivilegeMapper mapper;

  @Transactional
  @Override
  public PrivilegeDto create(CreatePrivilegeRequest req) {
    Privilege p = repository.save(mapper.toEntity(req));
    return mapper.toDto(p);
  }

  @Transactional
  @Override
  public PrivilegeDto update(Long id, UpdatePrivilegeRequest req) {
    Privilege p = repository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Privilege not found: " + id));
    mapper.updateEntity(p, req);
    return mapper.toDto(repository.save(p));
  }

  @Transactional
  @Override
  public void delete(Long id) {
    if (!repository.existsById(id)) return;
    repository.deleteById(id);
  }

  @Override
  public PrivilegeDto get(Long id) {
    return repository.findById(id).map(mapper::toDto)
        .orElseThrow(() -> new NoSuchElementException("Privilege not found: " + id));
  }

  @Override
  public List<PrivilegeDto> listByTenant(UUID tenantId) {
    return mapper.toDto(repository.findAllByTenantId(tenantId));
  }
}
