package com.ejada.catalog.service.impl;

import com.ejada.catalog.dto.FeatureCreateReq;
import com.ejada.catalog.dto.FeatureRes;
import com.ejada.catalog.dto.FeatureUpdateReq;
import com.ejada.catalog.mapper.FeatureMapper;
import com.ejada.catalog.model.Feature;
import com.ejada.catalog.repository.FeatureRepository;
import com.ejada.catalog.service.FeatureService;
import com.ejada.common.dto.BaseResponse;
import com.ejada.common.exception.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public  class FeatureServiceImpl implements FeatureService {

    private final FeatureRepository repo;
    private final FeatureMapper mapper;

    @Override
    public BaseResponse<FeatureRes> create(final FeatureCreateReq req) {
        if (repo.existsByFeatureKey(req.featureKey())) {
            throw new IllegalStateException("featureKey already exists: " + req.featureKey());
        }
        Feature e = mapper.toEntity(req);
        return BaseResponse.success("Feature created", mapper.toRes(repo.save(e)));
    }

    @Override
    public BaseResponse<FeatureRes> update(final Integer id, final FeatureUpdateReq req) {
        Feature e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Feature " + id));
        mapper.update(e, req);
        return BaseResponse.success("Feature updated", mapper.toRes(e));
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<FeatureRes> get(final Integer id) {
        Feature feature = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feature", String.valueOf(id)));
        return BaseResponse.success("OK", mapper.toRes(feature));
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<Page<FeatureRes>> list(final String category, final Pageable pageable) {
        Page<FeatureRes> page;
        if (category == null || category.isBlank()) {
            page = repo.findByIsDeletedFalse(pageable).map(mapper::toRes);
        } else {
            page = repo.findByCategoryAndIsDeletedFalse(category, pageable).map(mapper::toRes);
        }
        return BaseResponse.success("Feature page", page);
    }

    @Override
    public BaseResponse<Void> softDelete(final Integer id) {
        Feature e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Feature " + id));
        e.setIsDeleted(true);
        return BaseResponse.success("Feature deleted", null);
    }
}
