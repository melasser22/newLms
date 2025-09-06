package com.ejada.tenant.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.tenant.dto.*;
import com.ejada.tenant.mapper.TenantMapper;
import com.ejada.tenant.model.Tenant;
import com.ejada.tenant.repository.TenantRepository;
import com.ejada.tenant.service.TenantService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TenantServiceImpl implements TenantService {

    private final TenantRepository repo;
    private final TenantMapper mapper;

    @Override
    public BaseResponse<TenantRes> create(TenantCreateReq req) {
        if (repo.existsByCodeAndIsDeletedFalse(req.code())) {
            throw new IllegalStateException("tenant code exists: " + req.code());
        }
        if (repo.existsByNameIgnoreCaseAndIsDeletedFalse(req.name())) {
            throw new IllegalStateException("tenant name exists: " + req.name());
        }
        Tenant e = mapper.toEntity(req);
        e = repo.save(e);
        return BaseResponse.success("Tenant created", mapper.toRes(e));
    }

    @Override
    public BaseResponse<TenantRes> update(Integer id, TenantUpdateReq req) {
        Tenant e = repo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant " + id));

        if (req.code() != null && !req.code().equals(e.getCode())
                && repo.existsByCodeAndIdNot(req.code(), id)) {
            throw new IllegalStateException("tenant code exists: " + req.code());
        }
        if (req.name() != null && !req.name().equalsIgnoreCase(e.getName())
                && repo.existsByNameIgnoreCaseAndIdNot(req.name(), id)) {
            throw new IllegalStateException("tenant name exists: " + req.name());
        }

        mapper.update(e, req);
        // repo.save(e) is optional (managed entity), but keep it explicit:
        e = repo.save(e);
        return BaseResponse.success("Tenant updated", mapper.toRes(e));
    }

    @Override
    public BaseResponse<Void> softDelete(Integer id) {
        Tenant e = repo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant " + id));
        e.setIsDeleted(true);
        e.setActive(false);
        repo.save(e);
        return BaseResponse.success("Tenant deleted", null);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<TenantRes> get(Integer id) {
        Tenant e = repo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant " + id));
        return BaseResponse.success("Tenant fetched", mapper.toRes(e));
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<Page<TenantRes>> list(String name, Boolean active, Pageable pageable) {
        Page<Tenant> page;
        if ((name == null || name.isBlank()) && active == null) {
            page = repo.findByIsDeletedFalse(pageable);
        } else if (active == null) {
            page = repo.findByNameContainingIgnoreCaseAndIsDeletedFalse(name, pageable);
        } else if (name == null || name.isBlank()) {
            page = repo.findByActiveAndIsDeletedFalse(active, pageable);
        } else {
            page = repo.findByNameContainingIgnoreCaseAndActiveAndIsDeletedFalse(name, active, pageable);
        }
        return BaseResponse.success("Tenants listed", page.map(mapper::toRes));
    }
}