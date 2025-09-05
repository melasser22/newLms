package com.ejada.catalog.service.impl;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.mapper.AddonFeatureMapper;
import com.ejada.catalog.model.*;
import com.ejada.catalog.repository.AddonFeatureRepository;
import com.ejada.catalog.repository.AddonRepository;
import com.ejada.catalog.repository.FeatureRepository;
import com.ejada.catalog.service.AddonFeatureService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AddonFeatureServiceImpl implements AddonFeatureService {

    private final AddonFeatureRepository repo;
    private final AddonRepository addonRepo;
    private final FeatureRepository featureRepo;
    private final AddonFeatureMapper mapper;

    @Override
    public AddonFeatureRes attach(AddonFeatureCreateReq req) {
        addonRepo.findById(req.addonId()).orElseThrow(() -> new EntityNotFoundException("Addon " + req.addonId()));
        featureRepo.findById(req.featureId()).orElseThrow(() -> new EntityNotFoundException("Feature " + req.featureId()));

        if (repo.existsByAddon_AddonIdAndFeature_FeatureId(req.addonId(), req.featureId())) {
            throw new IllegalStateException("AddonFeature exists for addon=" + req.addonId() + " feature=" + req.featureId());
        }
        AddonFeature e = mapper.toEntity(req);
        return mapper.toRes(repo.save(e));
    }

    @Override
    public AddonFeatureRes update(Integer id, AddonFeatureUpdateReq req) {
        AddonFeature e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("AddonFeature " + id));
        mapper.update(e, req);
        return mapper.toRes(e);
    }

    @Override
    public void detach(Integer id) {
        AddonFeature e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("AddonFeature " + id));
        e.setIsDeleted(true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AddonFeatureRes> listByAddon(Integer addonId, Pageable pageable) {
        addonRepo.findById(addonId).orElseThrow(() -> new EntityNotFoundException("Addon " + addonId));
        return repo.findByAddon_AddonIdAndIsDeletedFalse(addonId, pageable).map(mapper::toRes);
    }
}
