package com.ejada.catalog.service.impl;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.mapper.AddonMapper;
import com.ejada.catalog.model.Addon;
import com.ejada.catalog.repository.AddonRepository;
import com.ejada.catalog.service.AddonService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
    public AddonRes create(AddonCreateReq req) {
        if (repo.existsByAddonCd(req.addonCd())) {
            throw new IllegalStateException("addonCd exists: " + req.addonCd());
        }
        Addon e = mapper.toEntity(req);
        return mapper.toRes(repo.save(e));
    }

    @Override
    public AddonRes update(Integer id, AddonUpdateReq req) {
        Addon e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Addon " + id));
        mapper.update(e, req);
        return mapper.toRes(e);
    }

    @Override
    @Transactional(readOnly = true)
    public AddonRes get(Integer id) {
        return mapper.toRes(repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Addon " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AddonRes> list(String category, Pageable pageable) {
        if (category == null || category.isBlank()) {
            return repo.findByIsDeletedFalse(pageable).map(mapper::toRes);
        }
        return repo.findByCategoryAndIsDeletedFalse(category, pageable).map(mapper::toRes);
    }

    @Override
    public void softDelete(Integer id) {
        Addon e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Addon " + id));
        e.setIsDeleted(true);
    }
}
