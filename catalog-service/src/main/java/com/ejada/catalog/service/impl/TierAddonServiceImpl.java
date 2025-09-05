package com.ejada.catalog.service.impl;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.mapper.TierAddonMapper;
import com.ejada.catalog.model.*;
import com.ejada.catalog.repository.AddonRepository;
import com.ejada.catalog.repository.TierAddonRepository;
import com.ejada.catalog.repository.TierRepository;
import com.ejada.catalog.service.TierAddonService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TierAddonServiceImpl implements TierAddonService {

    private final TierAddonRepository repo;
    private final TierRepository tierRepo;
    private final AddonRepository addonRepo;
    private final TierAddonMapper mapper;

    @Override
    public TierAddonRes allow(TierAddonCreateReq req) {
        tierRepo.findById(req.tierId()).orElseThrow(() -> new EntityNotFoundException("Tier " + req.tierId()));
        addonRepo.findById(req.addonId()).orElseThrow(() -> new EntityNotFoundException("Addon " + req.addonId()));

        if (repo.existsByTier_TierIdAndAddon_AddonId(req.tierId(), req.addonId())) {
            throw new IllegalStateException("TierAddon exists for tier=" + req.tierId() + " addon=" + req.addonId());
        }
        TierAddon e = mapper.toEntity(req);
        return mapper.toRes(repo.save(e));
    }

    @Override
    public TierAddonRes update(Integer id, TierAddonUpdateReq req) {
        TierAddon e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("TierAddon " + id));
        mapper.update(e, req);
        return mapper.toRes(e);
    }

    @Override
    public void remove(Integer id) {
        TierAddon e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("TierAddon " + id));
        e.setIsDeleted(true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TierAddonRes> listByTier(Integer tierId, Pageable pageable) {
        tierRepo.findById(tierId).orElseThrow(() -> new EntityNotFoundException("Tier " + tierId));
        return repo.findByTier_TierIdAndIsDeletedFalse(tierId, pageable).map(mapper::toRes);
    }
}
