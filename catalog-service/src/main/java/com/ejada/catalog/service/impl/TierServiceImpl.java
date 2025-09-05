package com.ejada.catalog.service.impl;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.mapper.TierMapper;
import com.ejada.catalog.model.Tier;
import com.ejada.catalog.repository.TierRepository;
import com.ejada.catalog.service.TierService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TierServiceImpl implements TierService {

    private final TierRepository repo;
    private final TierMapper mapper;

    @Override
    public TierRes create(TierCreateReq req) {
        if (repo.existsByTierCd(req.tierCd())) {
            throw new IllegalStateException("tierCd already exists: " + req.tierCd());
        }
        Tier entity = mapper.toEntity(req);
        return mapper.toRes(repo.save(entity));
    }

    @Override
    public TierRes update(Integer id, TierUpdateReq req) {
        Tier entity = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Tier " + id));
        mapper.update(entity, req);
        return mapper.toRes(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public TierRes get(Integer id) {
        return mapper.toRes(repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Tier " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TierRes> list(Boolean active, Pageable pageable) {
        if (active == null) return repo.findByIsDeletedFalse(pageable).map(mapper::toRes);
        return repo.findByIsActiveAndIsDeletedFalse(active, pageable).map(mapper::toRes);
    }

    @Override
    public void softDelete(Integer id) {
        Tier e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Tier " + id));
        e.setIsDeleted(true);
    }
}
