package com.ejada.catalog.service.impl;

import com.ejada.audit.starter.api.AuditAction;
import com.ejada.audit.starter.api.DataClass;
import com.ejada.audit.starter.api.annotations.Audited;
import com.ejada.catalog.dto.*;
import com.ejada.catalog.mapper.AddonMapper;
import com.ejada.catalog.model.Addon;
import com.ejada.catalog.repository.AddonRepository;
import com.ejada.catalog.service.AddonService;
import com.ejada.common.dto.BaseResponse;
import com.ejada.common.exception.DuplicateResourceException;
import com.ejada.common.exception.ResourceNotFoundException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AddonServiceImpl implements AddonService {

    private final AddonRepository repo;
    private final AddonMapper mapper;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "addons", allEntries = true)
    public BaseResponse<AddonRes> create(AddonCreateReq req) {
        Addon e = mapper.toEntity(req);
        try {
            Addon saved = repo.save(e);
            return BaseResponse.success("Addon created", mapper.toRes(saved));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Addon", req.addonCd());
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "addons", key = "#id")
    public BaseResponse<AddonRes> update(Integer id, AddonUpdateReq req) {
        Addon e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Addon " + id));
        mapper.update(e, req);
        try {
            Addon saved = repo.save(e);
            return BaseResponse.success("Addon updated", mapper.toRes(saved));
        } catch (DataIntegrityViolationException ex) {
            if (req.addonCd() != null) {
                throw new DuplicateResourceException("Addon", req.addonCd());
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "addons", key = "#id")
    public BaseResponse<AddonRes> get(Integer id) {
        Addon addon = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Addon", String.valueOf(id)));
        return BaseResponse.success("OK", mapper.toRes(addon));

    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entity = "Addon", dataClass = DataClass.HEALTH, message = "List Addons")
    public BaseResponse<Page<AddonRes>> list(String category, Pageable pageable) {
        if (category == null || category.isBlank()) {
            return BaseResponse.success("Addon page", repo.findByIsDeletedFalse(pageable).map(mapper::toRes));

        }
        return BaseResponse.success("Addon page", repo.findByCategoryAndIsDeletedFalse(category, pageable).map(mapper::toRes));

    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "addons", key = "#id")
    public BaseResponse<Void> softDelete(Integer id) {
        Addon e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Addon " + id));
        e.setIsDeleted(true);
        repo.save(e);
        return BaseResponse.success("Addon deleted", null);
    }
}
