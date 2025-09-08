package com.ejada.tenant.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.crypto.CryptoFacade;
import com.ejada.tenant.dto.TenantIntegrationKeyCreateReq;
import com.ejada.tenant.dto.TenantIntegrationKeyRes;
import com.ejada.tenant.dto.TikStatus;
import com.ejada.tenant.mapper.TenantIntegrationKeyMapper;
import com.ejada.tenant.model.Tenant;
import com.ejada.tenant.model.TenantIntegrationKey;
import com.ejada.tenant.repository.TenantIntegrationKeyRepository;
import com.ejada.tenant.repository.TenantRepository;
import com.ejada.tenant.service.impl.TenantIntegrationKeyServiceImpl;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TenantIntegrationKeyServiceImplTest {

    @Test
    void createGeneratesSecretWhenMissing() throws Exception {
        TenantIntegrationKeyRepository repo = mock(TenantIntegrationKeyRepository.class);
        TenantRepository tenantRepo = mock(TenantRepository.class);
        TenantIntegrationKeyMapper mapper = Mappers.getMapper(TenantIntegrationKeyMapper.class);
        CryptoFacade crypto = mock(CryptoFacade.class);

        Tenant tenant = new Tenant();
        tenant.setId(1);
        when(tenantRepo.findByIdAndIsDeletedFalse(1)).thenReturn(Optional.of(tenant));
        when(repo.existsByTenant_IdAndKeyIdAndIsDeletedFalse(1, "KEY")).thenReturn(false);
        when(crypto.signToBase64(anyString())).thenReturn("hashed-secret");
        when(repo.save(any(TenantIntegrationKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantIntegrationKeyCreateReq req = new TenantIntegrationKeyCreateReq(
                1, "KEY", null, "label", List.of(), TikStatus.ACTIVE,
                null, OffsetDateTime.now().plusDays(1), null, null
        );

        TenantIntegrationKeyServiceImpl service =
                new TenantIntegrationKeyServiceImpl(repo, tenantRepo, mapper, crypto);

        BaseResponse<TenantIntegrationKeyRes> resp = service.create(req);
        assertNotNull(resp.getData().plainSecret());
        assertFalse(resp.getData().plainSecret().isBlank());

        ArgumentCaptor<TenantIntegrationKey> captor = ArgumentCaptor.forClass(TenantIntegrationKey.class);
        verify(repo).save(captor.capture());
        assertEquals("hashed-secret", captor.getValue().getKeySecret());
    }
}

