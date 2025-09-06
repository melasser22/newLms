package com.ejada.catalog.service.impl;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.mapper.TierFeatureMapper;
import com.ejada.catalog.model.*;
import com.ejada.catalog.repository.FeatureRepository;
import com.ejada.catalog.repository.TierFeatureRepository;
import com.ejada.catalog.repository.TierRepository;
import com.ejada.catalog.service.TierFeatureService;
import com.ejada.common.dto.BaseResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TierFeatureServiceImpl implements TierFeatureService {

    private final TierFeatureRepository repo;
    private final TierRepository tierRepo;
    private final FeatureRepository featureRepo;
    private final TierFeatureMapper mapper;

    @Override
    public BaseResponse<TierFeatureRes> attach(TierFeatureCreateReq req) {
        // validate existence
        tierRepo.findById(req.tierId()).orElseThrow(() -> new EntityNotFoundException("Tier " + req.tierId()));
        featureRepo.findById(req.featureId()).orElseThrow(() -> new EntityNotFoundException("Feature " + req.featureId()));

        if (repo.existsByTier_TierIdAndFeature_FeatureId(req.tierId(), req.featureId())) {
            throw new IllegalStateException("TierFeature already exists for tier=" + req.tierId() + " feature=" + req.featureId());
        }
        TierFeature e = mapper.toEntity(req);
        return BaseResponse.success("Tier feature attached", mapper.toRes(repo.save(e)));
    }

    @Override
    public BaseResponse<TierFeatureRes> update(Integer id, TierFeatureUpdateReq req) {
        TierFeature e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("TierFeature " + id));
        mapper.update(e, req);
        return BaseResponse.success("Tier feature updated", mapper.toRes(e));
    }

    @Override
    public BaseResponse<Void> detach(Integer id) {
        TierFeature e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("TierFeature " + id));
        e.setIsDeleted(true);
        return BaseResponse.success("Tier feature detached", null);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<Page<TierFeatureRes>> listByTier(Integer tierId, Pageable pageable) {
        tierRepo.findById(tierId).orElseThrow(() -> new EntityNotFoundException("Tier " + tierId));
        Page<TierFeatureRes> page = repo.findByTier_TierIdAndIsDeletedFalse(tierId, pageable).map(mapper::toRes);
        return BaseResponse.success("Tier feature page", page);
    }
}
