package com.ejada.tenant.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ejada.common.dto.BaseResponse;
import com.ejada.crypto.CryptoFacade;
import com.ejada.tenant.dto.TenantIntegrationKeyRes;
import com.ejada.tenant.dto.TenantIntegrationKeyUpdateReq;
import com.ejada.tenant.dto.TikStatus;
import com.ejada.tenant.mapper.TenantIntegrationKeyMapper;
import com.ejada.tenant.model.TenantIntegrationKey;
import com.ejada.tenant.model.TenantIntegrationKey.Status;
import com.ejada.tenant.repository.TenantIntegrationKeyRepository;
import com.ejada.tenant.repository.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantIntegrationKeyServiceImplTest {

  @Mock private TenantIntegrationKeyRepository repository;
  @Mock private TenantRepository tenantRepository;
  @Mock private TenantIntegrationKeyMapper mapper;
  @Mock private CryptoFacade cryptoFacade;

  private TenantIntegrationKeyServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TenantIntegrationKeyServiceImpl(repository, tenantRepository, mapper, cryptoFacade);
  }

  @Test
  void updateValidatesWindowAndThrowsWhenInvalid() {
    TenantIntegrationKey existing = new TenantIntegrationKey();
    existing.setTikId(1L);
    existing.setValidFrom(OffsetDateTime.now().plusDays(1));
    existing.setExpiresAt(OffsetDateTime.now().plusDays(2));
    when(repository.findByTikIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(existing));

    TenantIntegrationKeyUpdateReq req = new TenantIntegrationKeyUpdateReq(null, null, OffsetDateTime.now());

    assertThatThrownBy(() -> service.update(1L, req))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("expiresAt must be after validFrom");
  }

  @Test
  void updateAutoExpiresWhenWindowElapsed() {
    TenantIntegrationKey existing = new TenantIntegrationKey();
    existing.setTikId(2L);
    existing.setValidFrom(OffsetDateTime.now().minusDays(5));
    existing.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
    existing.setStatus(Status.ACTIVE);
    when(repository.findByTikIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(existing));
    when(repository.save(any(TenantIntegrationKey.class))).thenAnswer(invocation -> invocation.getArgument(0));
    TenantIntegrationKeyRes mapped = new TenantIntegrationKeyRes(
        2L,
        3,
        "key",
        "desc",
        null,
        TikStatus.EXPIRED,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
    when(mapper.toRes(any(TenantIntegrationKey.class))).thenReturn(mapped);

    BaseResponse<TenantIntegrationKeyRes> response = service.update(2L, new TenantIntegrationKeyUpdateReq(null, null, null));

    assertThat(response.isSuccess()).isTrue();
    assertThat(response.getData().status()).isEqualTo(TikStatus.EXPIRED);
  }

  @Test
  void updateThrowsWhenKeyMissing() {
    when(repository.findByTikIdAndIsDeletedFalse(55L)).thenReturn(Optional.empty());

    TenantIntegrationKeyUpdateReq req = new TenantIntegrationKeyUpdateReq(null, null, null);

    assertThatThrownBy(() -> service.update(55L, req))
        .isInstanceOf(EntityNotFoundException.class);
  }
}
