package com.ejada.tenant.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.common.dto.BaseResponse;
import com.ejada.tenant.dto.TenantCreateReq;
import com.ejada.tenant.dto.TenantRes;
import com.ejada.tenant.dto.TenantUpdateReq;
import com.ejada.tenant.exception.TenantConflictException;
import com.ejada.tenant.mapper.TenantMapper;
import com.ejada.tenant.model.Tenant;
import com.ejada.tenant.repository.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantServiceImplTest {

  @Mock private TenantRepository repository;
  @Mock private TenantMapper mapper;

  private TenantServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TenantServiceImpl(repository, mapper);
  }

  @Test
  void createFailsWhenDuplicateCodeExists() {
    TenantCreateReq req = new TenantCreateReq("CODE", "Tenant", null, null, null, true);
    when(repository.existsByCodeAndIsDeletedFalse("CODE")).thenReturn(true);

    assertThatThrownBy(() -> service.create(req))
        .isInstanceOf(TenantConflictException.class)
        .hasMessageContaining("tenant code exists");
  }

  @Test
  void createFailsWhenDuplicateNameExists() {
    TenantCreateReq req = new TenantCreateReq("CODE", "Tenant", null, null, null, true);
    when(repository.existsByCodeAndIsDeletedFalse("CODE")).thenReturn(false);
    when(repository.existsByNameIgnoreCaseAndIsDeletedFalse("Tenant")).thenReturn(true);

    assertThatThrownBy(() -> service.create(req))
        .isInstanceOf(TenantConflictException.class)
        .hasMessageContaining("tenant name exists");
  }

  @Test
  void updateRejectsDuplicateCodeForDifferentTenant() {
    Tenant existing = new Tenant();
    existing.setId(5);
    existing.setCode("OLD");
    when(repository.findByIdAndIsDeletedFalse(5)).thenReturn(Optional.of(existing));
    when(repository.existsByCodeAndIdNot("NEW", 5)).thenReturn(true);

    TenantUpdateReq req = new TenantUpdateReq("NEW", null, null, null, null, null);

    assertThatThrownBy(() -> service.update(5, req))
        .isInstanceOf(TenantConflictException.class)
        .hasMessageContaining("tenant code exists");
  }

  @Test
  void updateRejectsDuplicateNameForDifferentTenant() {
    Tenant existing = new Tenant();
    existing.setId(7);
    existing.setName("Current");
    when(repository.findByIdAndIsDeletedFalse(7)).thenReturn(Optional.of(existing));
    when(repository.existsByNameIgnoreCaseAndIdNot("NewName", 7)).thenReturn(true);

    TenantUpdateReq req = new TenantUpdateReq(null, "NewName", null, null, null, null);

    assertThatThrownBy(() -> service.update(7, req))
        .isInstanceOf(TenantConflictException.class)
        .hasMessageContaining("tenant name exists");
  }

  @Test
  void updateReturnsSuccessWhenNoConflicts() {
    Tenant existing = new Tenant();
    existing.setId(11);
    existing.setCode("CODE");
    existing.setName("Name");
    when(repository.findByIdAndIsDeletedFalse(11)).thenReturn(Optional.of(existing));
    when(repository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
    TenantRes mapped = new TenantRes(11, "CODE", "Name", null, null, null, true, false, null, null);
    when(mapper.toRes(any(Tenant.class))).thenReturn(mapped);

    TenantUpdateReq req = new TenantUpdateReq(null, "Updated", null, null, null, null);

    BaseResponse<TenantRes> response = service.update(11, req);

    assertThat(response.isSuccess()).isTrue();
    assertThat(response.getData().name()).isEqualTo("Name");
  }

  @Test
  void updateThrowsWhenTenantMissing() {
    when(repository.findByIdAndIsDeletedFalse(99)).thenReturn(Optional.empty());

    TenantUpdateReq req = new TenantUpdateReq(null, null, null, null, null, null);

    assertThatThrownBy(() -> service.update(99, req))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void softDeleteMarksTenantInactiveAndDeleted() {
    Tenant tenant = new Tenant();
    tenant.setId(44);
    tenant.setActive(true);
    tenant.setIsDeleted(false);
    when(repository.findByIdAndIsDeletedFalse(44)).thenReturn(Optional.of(tenant));

    service.softDelete(44);

    assertThat(tenant.getIsDeleted()).isTrue();
    assertThat(tenant.getActive()).isFalse();
    verify(repository).save(tenant);
  }
}
