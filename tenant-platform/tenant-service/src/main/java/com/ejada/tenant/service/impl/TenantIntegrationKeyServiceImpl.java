package com.ejada.tenant.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.crypto.CryptoFacade;
import com.ejada.tenant.dto.*;
import com.ejada.tenant.mapper.TenantIntegrationKeyMapper;
import com.ejada.tenant.model.Tenant;
import com.ejada.tenant.model.TenantIntegrationKey;
import com.ejada.tenant.model.TenantIntegrationKey.Status;
import com.ejada.tenant.repository.TenantIntegrationKeyRepository;
import com.ejada.tenant.repository.TenantRepository;
import com.ejada.tenant.service.TenantIntegrationKeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
@Transactional
public class TenantIntegrationKeyServiceImpl implements TenantIntegrationKeyService {

    private final TenantIntegrationKeyRepository repo;
    private final TenantRepository tenantRepo;
    private final TenantIntegrationKeyMapper mapper;
    private final CryptoFacade crypto;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public TenantIntegrationKeyServiceImpl(TenantIntegrationKeyRepository repo,
                                           TenantRepository tenantRepo,
                                           TenantIntegrationKeyMapper mapper,
                                           CryptoFacade crypto) {
        this.repo = repo;
        this.tenantRepo = tenantRepo;
        this.mapper = mapper;
        this.crypto = crypto;
    }

    @Override
    public BaseResponse<TenantIntegrationKeyRes> create(TenantIntegrationKeyCreateReq req) {
        // Validate tenant
        Tenant tenant = tenantRepo.findByIdAndIsDeletedFalse(req.tenantId())
                .orElseThrow(() -> new EntityNotFoundException("Tenant " + req.tenantId()));

        // Uniqueness under soft delete
        if (repo.existsByTenant_IdAndKeyIdAndIsDeletedFalse(req.tenantId(), req.keyId())) {
            throw new IllegalStateException("integration key exists for tenant=" + req.tenantId() + " keyId=" + req.keyId());
        }

        // Basic window validation (controller has Bean Validation, but double-check here)
        OffsetDateTime validFrom = (req.validFrom() != null) ? req.validFrom() : OffsetDateTime.now();
        if (req.expiresAt() == null || !req.expiresAt().isAfter(validFrom)) {
            throw new IllegalArgumentException("expiresAt must be after validFrom");
        }

        // Map
        TenantIntegrationKey e = mapper.toEntity(req);
        e.setTenant(tenant);
        e.setValidFrom(validFrom);

        // Secret handling
        String plainSecret = req.plainSecret();
        if (plainSecret == null || plainSecret.isBlank()) {
            byte[] secretBytes = new byte[32];
            new SecureRandom().nextBytes(secretBytes);
            plainSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        }
        try {
            e.setKeySecret(crypto.signToBase64(plainSecret));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign integration key secret", ex);
        }

        e = repo.save(e);
        TenantIntegrationKeyRes res = mapper.toRes(e).withPlainSecret(plainSecret);
        return BaseResponse.success("Tenant integration key created", res);
    }

    @Override
    public BaseResponse<TenantIntegrationKeyRes> update(Long tikId, TenantIntegrationKeyUpdateReq req) {
        TenantIntegrationKey e = repo.findByTikIdAndIsDeletedFalse(tikId)
                .orElseThrow(() -> new EntityNotFoundException("TenantIntegrationKey " + tikId));

        // Validate window if either side is changing
        OffsetDateTime vf = req.validFrom() != null ? req.validFrom() : e.getValidFrom();
        OffsetDateTime ex = req.expiresAt() != null ? req.expiresAt() : e.getExpiresAt();
        if (vf != null && ex != null && !ex.isAfter(vf)) {
            throw new IllegalArgumentException("expiresAt must be after validFrom");
        }

        // Apply patch
        mapper.update(e, req);

        // Auto-mark EXPIRED if window says so
        if (e.getExpiresAt() != null && !e.getExpiresAt().isAfter(OffsetDateTime.now())) {
            e.setStatus(Status.EXPIRED);
        }

        e = repo.save(e);
        return BaseResponse.success("Tenant integration key updated", mapper.toRes(e));
    }

    @Override
    public BaseResponse<Void> revoke(Long tikId) {
        TenantIntegrationKey e = repo.findByTikIdAndIsDeletedFalse(tikId)
                .orElseThrow(() -> new EntityNotFoundException("TenantIntegrationKey " + tikId));
        e.setIsDeleted(true);
        e.setStatus(Status.REVOKED);
        repo.save(e);
        return BaseResponse.success("Tenant integration key revoked", null);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<TenantIntegrationKeyRes> get(Long tikId) {
        TenantIntegrationKey e = repo.findByTikIdAndIsDeletedFalse(tikId)
                .orElseThrow(() -> new EntityNotFoundException("TenantIntegrationKey " + tikId));
        return BaseResponse.success("Tenant integration key fetched", mapper.toRes(e));
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<Page<TenantIntegrationKeyRes>> listByTenant(Integer tenantId, Pageable pageable) {
        // Validate tenant existence (optional but helpful)
        tenantRepo.findByIdAndIsDeletedFalse(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant " + tenantId));
        Page<TenantIntegrationKeyRes> page =
                repo.findByTenant_IdAndIsDeletedFalse(tenantId, pageable).map(mapper::toRes);
        return BaseResponse.success("Tenant integration keys listed", page);
    }
}