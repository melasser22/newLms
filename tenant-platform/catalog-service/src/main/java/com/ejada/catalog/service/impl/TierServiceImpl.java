package com.ejada.catalog.service.impl;

import com.ejada.catalog.dto.TierCreateReq;
import com.ejada.catalog.dto.TierRes;
import com.ejada.catalog.dto.TierUpdateReq;
import com.ejada.catalog.mapper.TierMapper;
import com.ejada.catalog.model.Tier;
import com.ejada.catalog.repository.TierRepository;
import com.ejada.catalog.service.TierService;
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
public class TierServiceImpl implements TierService {

    private final TierRepository repo;
    private final TierMapper mapper;

    @Override
    public BaseResponse<TierRes> create(final TierCreateReq req) {
        if (repo.existsByTierCd(req.tierCd())) {
            throw new IllegalStateException("tierCd already exists: " + req.tierCd());
        }
        Tier entity = mapper.toEntity(req);
        return BaseResponse.success("Tier created", mapper.toRes(repo.save(entity)));
    }

    @Override
    public BaseResponse<TierRes> update(final Integer id, final TierUpdateReq req) {
        Tier entity = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Tier " + id));
        mapper.update(entity, req);
        return BaseResponse.success("Tier updated", mapper.toRes(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<TierRes> get(final Integer id) {
        return BaseResponse.success(
            "OK",
            mapper.toRes(repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Tier " + id)))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<Page<TierRes>> list(final Boolean active, final Pageable pageable) {
        Page<TierRes> page;
        if (active == null) {
            page = repo.findByIsDeletedFalse(pageable).map(mapper::toRes);
        } else {
            page = repo.findByIsActiveAndIsDeletedFalse(active, pageable).map(mapper::toRes);
        }
        return BaseResponse.success("Tier page", page);
    }

    @Override
    public BaseResponse<Void> softDelete(final Integer id) {
        Tier e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Tier " + id));
        e.setIsDeleted(true);
        return BaseResponse.success("Tier deleted", null);
    }
}
