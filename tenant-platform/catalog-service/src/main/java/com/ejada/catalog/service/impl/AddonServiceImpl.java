package com.ejada.catalog.service.impl;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.mapper.AddonMapper;
import com.ejada.catalog.model.Addon;
import com.ejada.catalog.repository.AddonRepository;
import com.ejada.catalog.service.AddonService;
import com.ejada.common.dto.BaseResponse;
import com.ejada.common.service.BaseCrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for {@link Addon} operations.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AddonServiceImpl extends BaseCrudService<Addon, Integer, AddonCreateReq, AddonUpdateReq, AddonRes>
        implements AddonService {

    private final AddonRepository repo;
    private final AddonMapper mapper;

    @Override
    protected AddonRepository getRepository() {
        return repo;
    }

    @Override
    protected boolean existsByUniqueField(AddonCreateReq dto) {
        return repo.existsByAddonCd(dto.addonCd());
    }

    @Override
    protected Addon mapToEntity(AddonCreateReq dto) {
        return mapper.toEntity(dto);
    }

    @Override
    protected void updateEntity(Addon entity, AddonUpdateReq dto) {
        mapper.update(entity, dto);
    }

    @Override
    protected AddonRes mapToDto(Addon entity) {
        return mapper.toRes(entity);
    }

    @Override
    protected String getEntityName() {
        return "Addon";
    }

    @Override
    @Cacheable(cacheNames = "addons", key = "#id")
    public BaseResponse<AddonRes> get(Integer id) {
        return super.get(id);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<Page<AddonRes>> list(String category, Pageable pageable) {
        Page<Addon> page = (category == null || category.isBlank())
                ? repo.findByIsDeletedFalse(pageable)
                : repo.findByCategoryAndIsDeletedFalse(category, pageable);
        return BaseResponse.success("Addon page", page.map(mapper::toRes));
    }

    @Override
    @CacheEvict(cacheNames = "addons", key = "#id")
    public BaseResponse<AddonRes> update(Integer id, AddonUpdateReq req) {
        return super.update(id, req);
    }

    @Override
    @CacheEvict(cacheNames = "addons", key = "#id")
    public BaseResponse<Void> softDelete(Integer id) {
        return super.softDelete(id);
    }
}

