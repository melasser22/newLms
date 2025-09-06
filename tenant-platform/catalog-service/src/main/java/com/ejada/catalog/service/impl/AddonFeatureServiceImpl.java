package com.ejada.catalog.service.impl;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.mapper.AddonFeatureMapper;
import com.ejada.catalog.model.*;
import com.ejada.catalog.repository.AddonFeatureRepository;
import com.ejada.catalog.repository.AddonRepository;
import com.ejada.catalog.repository.FeatureRepository;
import com.ejada.catalog.service.AddonFeatureService;
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
public class AddonFeatureServiceImpl implements AddonFeatureService {

    private final AddonFeatureRepository repo;
    private final AddonRepository addonRepo;
    private final FeatureRepository featureRepo;
    private final AddonFeatureMapper mapper;

    @Override
    public BaseResponse<AddonFeatureRes> attach(AddonFeatureCreateReq req) {
        addonRepo.findById(req.addonId()).orElseThrow(() -> new EntityNotFoundException("Addon " + req.addonId()));
        featureRepo.findById(req.featureId()).orElseThrow(() -> new EntityNotFoundException("Feature " + req.featureId()));

        if (repo.existsByAddon_AddonIdAndFeature_FeatureId(req.addonId(), req.featureId())) {
            throw new IllegalStateException("AddonFeature exists for addon=" + req.addonId() + " feature=" + req.featureId());
        }
        AddonFeature e = mapper.toEntity(req);
        return BaseResponse.success("Addon feature attached", mapper.toRes(repo.save(e)));
    }

    @Override
    public BaseResponse<AddonFeatureRes> update(Integer id, AddonFeatureUpdateReq req) {
        AddonFeature e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("AddonFeature " + id));
        mapper.update(e, req);
        return BaseResponse.success("Addon feature updated", mapper.toRes(e));
    }

    @Override
    public BaseResponse<Void> detach(Integer id) {
        AddonFeature e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("AddonFeature " + id));
        e.setIsDeleted(true);
        return BaseResponse.success("Addon feature detached", null);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<Page<AddonFeatureRes>> listByAddon(Integer addonId, Pageable pageable) {
        addonRepo.findById(addonId).orElseThrow(() -> new EntityNotFoundException("Addon " + addonId));
        Page<AddonFeatureRes> page = repo.findByAddon_AddonIdAndIsDeletedFalse(addonId, pageable).map(mapper::toRes);
        return BaseResponse.success("Addon feature page", page);
    }
}
