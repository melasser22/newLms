package com.ejada.catalog.service.impl;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.mapper.TierFeatureMapper;
import com.ejada.catalog.model.*;
import com.ejada.catalog.repository.FeatureRepository;
import com.ejada.catalog.repository.TierFeatureRepository;
import com.ejada.catalog.repository.TierRepository;
import com.ejada.catalog.service.TierFeatureService;
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
    public TierFeatureRes attach(TierFeatureCreateReq req) {
        // validate existence
        tierRepo.findById(req.tierId()).orElseThrow(() -> new EntityNotFoundException("Tier " + req.tierId()));
        featureRepo.findById(req.featureId()).orElseThrow(() -> new EntityNotFoundException("Feature " + req.featureId()));

        if (repo.existsByTier_TierIdAndFeature_FeatureId(req.tierId(), req.featureId())) {
            throw new IllegalStateException("TierFeature already exists for tier=" + req.tierId() + " feature=" + req.featureId());
        }
        TierFeature e = mapper.toEntity(req);
        return mapper.toRes(repo.save(e));
    }

    @Override
    public TierFeatureRes update(Integer id, TierFeatureUpdateReq req) {
        TierFeature e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("TierFeature " + id));
        mapper.update(e, req);
        return mapper.toRes(e);
    }

    @Override
    public void detach(Integer id) {
        TierFeature e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("TierFeature " + id));
        e.setIsDeleted(true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TierFeatureRes> listByTier(Integer tierId, Pageable pageable) {
        tierRepo.findById(tierId).orElseThrow(() -> new EntityNotFoundException("Tier " + tierId));
        return repo.findByTier_TierIdAndIsDeletedFalse(tierId, pageable).map(mapper::toRes);
    }
}
