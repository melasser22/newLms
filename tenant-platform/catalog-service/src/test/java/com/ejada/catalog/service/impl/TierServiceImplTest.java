package com.ejada.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.catalog.dto.TierCreateReq;
import com.ejada.catalog.dto.TierRes;
import com.ejada.catalog.dto.TierUpdateReq;
import com.ejada.catalog.exception.CatalogConflictException;
import com.ejada.catalog.mapper.TierMapper;
import com.ejada.catalog.model.Tier;
import com.ejada.catalog.repository.TierRepository;
import com.ejada.common.dto.BaseResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TierServiceImplTest {

    @Mock private TierRepository repository;
    @Mock private TierMapper mapper;

    private TierServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TierServiceImpl(repository, mapper);
    }

    @Test
    void createFailsWhenTierCodeExists() {
        TierCreateReq req = new TierCreateReq("BASIC", "Basic", "الأساسي", "desc", 1, true);
        when(repository.existsByTierCd("BASIC")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("tierCd already exists");
    }

    @Test
    void createPersistsWhenUnique() {
        TierCreateReq req = new TierCreateReq("BASIC", "Basic", "الأساسي", "desc", 1, true);
        Tier entity = new Tier();
        when(mapper.toEntity(req)).thenReturn(entity);
        Tier persisted = new Tier();
        when(repository.save(entity)).thenReturn(persisted);
        TierRes res = new TierRes(1, "BASIC", "Basic", "الأساسي", "desc", 1, true, false);
        when(mapper.toRes(persisted)).thenReturn(res);

        BaseResponse<TierRes> response = service.create(req);

        assertThat(response.getData()).isEqualTo(res);
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void softDeleteMarksTierInactive() {
        Tier tier = new Tier();
        tier.setTierId(5);
        tier.setIsActive(true);
        tier.setIsDeleted(false);
        when(repository.findById(5)).thenReturn(Optional.of(tier));

        service.softDelete(5);

        assertThat(tier.getIsDeleted()).isTrue();
        assertThat(tier.getIsActive()).isFalse();
        verify(repository).findById(5);
    }

    @Test
    void softDeleteThrowsWhenMissing() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.softDelete(99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateDelegatesToMapper() {
        Tier tier = new Tier();
        when(repository.findById(10)).thenReturn(Optional.of(tier));
        when(mapper.toRes(tier)).thenReturn(new TierRes(10, "CODE", "EN", "AR", "desc", 1, true, false));

        TierUpdateReq req = new TierUpdateReq("CODE", "EN", "AR", "desc", 1, true);
        BaseResponse<TierRes> response = service.update(10, req);

        verify(mapper).update(tier, req);
        assertThat(response.isSuccess()).isTrue();
    }
}
