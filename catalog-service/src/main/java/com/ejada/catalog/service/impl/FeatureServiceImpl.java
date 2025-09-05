package com.ejada.catalog.service.impl;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.mapper.FeatureMapper;
import com.ejada.catalog.model.Feature;
import com.ejada.catalog.repository.FeatureRepository;
import com.ejada.catalog.service.FeatureService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FeatureServiceImpl implements FeatureService {

    private final FeatureRepository repo;
    private final FeatureMapper mapper;

    @Override
    public FeatureRes create(FeatureCreateReq req) {
        if (repo.existsByFeatureKey(req.featureKey())) {
            throw new IllegalStateException("featureKey already exists: " + req.featureKey());
        }
        Feature e = mapper.toEntity(req);
        return mapper.toRes(repo.save(e));
    }

    @Override
    public FeatureRes update(Integer id, FeatureUpdateReq req) {
        Feature e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Feature " + id));
        mapper.update(e, req);
        return mapper.toRes(e);
    }

    @Override
    @Transactional(readOnly = true)
    public FeatureRes get(Integer id) {
        return mapper.toRes(repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Feature " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeatureRes> list(String category, Pageable pageable) {
        if (category == null || category.isBlank()) {
            return repo.findByIsDeletedFalse(pageable).map(mapper::toRes);
        }
        return repo.findByCategoryAndIsDeletedFalse(category, pageable).map(mapper::toRes);
    }

    @Override
    public void softDelete(Integer id) {
        Feature e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Feature " + id));
        e.setIsDeleted(true);
    }
}
