package com.ejada.tenant.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.crypto.CryptoFacade;
import com.ejada.tenant.dto.TenantIntegrationKeyCreateReq;
import com.ejada.tenant.dto.TenantIntegrationKeyRes;
import com.ejada.tenant.dto.TenantIntegrationKeyUpdateReq;
import com.ejada.tenant.mapper.TenantIntegrationKeyMapper;
import com.ejada.tenant.model.Tenant;
import com.ejada.tenant.model.TenantIntegrationKey;
import com.ejada.tenant.model.TenantIntegrationKey.Status;
import com.ejada.tenant.repository.TenantIntegrationKeyRepository;
import com.ejada.tenant.repository.TenantRepository;
import com.ejada.tenant.service.TenantIntegrationKeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantIntegrationKeyServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SECRET_BYTES_LENGTH = 32;
    private static final int MIN_SECRET_LENGTH = 32;
    private static final int MIN_DISTINCT_CHARS = 6;
    private static final String DEFAULT_ACTOR = "system";
    private final TenantIntegrationKeyRepository repo;
    private final TenantRepository tenantRepo;
    private final TenantIntegrationKeyMapper mapper;
    private final CryptoFacade crypto;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public TenantIntegrationKeyServiceImpl(final TenantIntegrationKeyRepository repo,
                                           final TenantRepository tenantRepo,
                                           final TenantIntegrationKeyMapper mapper,
                                           final CryptoFacade crypto) {
        this.repo = repo;
        this.tenantRepo = tenantRepo;
        this.mapper = mapper;
        this.crypto = crypto;
    }

    @Override
    public BaseResponse<TenantIntegrationKeyRes> create(final TenantIntegrationKeyCreateReq req) {
        // Validate tenant
        Tenant tenant = tenantRepo.findByIdAndIsDeletedFalse(req.tenantId())
                .orElseThrow(() -> new EntityNotFoundException("Tenant " + req.tenantId()));

        // Uniqueness under soft delete
        if (repo.existsByTenantIdAndKeyIdAndIsDeletedFalse(req.tenantId(), req.keyId())) {
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

        String actor = resolveActor(req.createdBy());
        e.setCreatedBy(actor);

        String plainSecret = req.plainSecret();
        String resolvedSecret;
        try {
            if (StringUtils.isBlank(plainSecret)) {
                resolvedSecret = generateSecret();
            } else {
                resolvedSecret = validateSecret(plainSecret);
            }
        } catch (IllegalArgumentException policyEx) {
            LOGGER.warn("Secret policy violation on create: {}", policyEx.getMessage());
            return BaseResponse.error("ERR_TIK_SECRET_POLICY", policyEx.getMessage());
        }

        try {
            e.setKeySecret(crypto.signToBase64(resolvedSecret));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign integration key secret", ex);
        }
        OffsetDateTime rotationTs = OffsetDateTime.now();
        e.setSecretLastRotatedAt(rotationTs);
        e.setSecretLastRotatedBy(actor);

        e = repo.save(e);
        TenantIntegrationKeyRes res = mapper.toRes(e)
                .withPlainSecret(resolvedSecret);
        return BaseResponse.success("Tenant integration key created", res);
    }

    @Override
    public BaseResponse<TenantIntegrationKeyRes> update(final Long tikId, final TenantIntegrationKeyUpdateReq req) {
        TenantIntegrationKey e = repo.findByTikIdAndIsDeletedFalse(tikId)
                .orElseThrow(() -> new EntityNotFoundException("TenantIntegrationKey " + tikId));

        // Validate window if either side is changing
        OffsetDateTime vf = req.validFrom() != null ? req.validFrom() : e.getValidFrom();
        OffsetDateTime effectiveExpiresAt = req.expiresAt() != null ? req.expiresAt() : e.getExpiresAt();
        if (vf != null && effectiveExpiresAt != null && !effectiveExpiresAt.isAfter(vf)) {
            throw new IllegalArgumentException("expiresAt must be after validFrom");
        }

        // Apply patch
        mapper.update(e, req);

        String rotatedSecret = null;
        if (StringUtils.isNotBlank(req.newPlainSecret())) {
            try {
                rotatedSecret = validateSecret(req.newPlainSecret());
            } catch (IllegalArgumentException policyEx) {
                LOGGER.warn("Secret policy violation on update: {}", policyEx.getMessage());
                return BaseResponse.error("ERR_TIK_SECRET_POLICY", policyEx.getMessage());
            }
            try {
                e.setKeySecret(crypto.signToBase64(rotatedSecret));
            } catch (Exception ex) {
                throw new IllegalStateException("Could not sign integration key secret", ex);
            }
            e.setSecretLastRotatedAt(OffsetDateTime.now());
            e.setSecretLastRotatedBy(resolveActor(req.rotatedBy()));
        }

        // Auto-mark EXPIRED if window says so
        if (e.getExpiresAt() != null && !e.getExpiresAt().isAfter(OffsetDateTime.now())) {
            e.setStatus(Status.EXPIRED);
        }

        e = repo.save(e);
        TenantIntegrationKeyRes res = mapper.toRes(e);
        if (rotatedSecret != null) {
            res = res.withPlainSecret(rotatedSecret);
        }
        return BaseResponse.success("Tenant integration key updated", res);
    }

    @Override
    public BaseResponse<Void> revoke(final Long tikId) {
        TenantIntegrationKey e = repo.findByTikIdAndIsDeletedFalse(tikId)
                .orElseThrow(() -> new EntityNotFoundException("TenantIntegrationKey " + tikId));
        e.setIsDeleted(true);
        e.setStatus(Status.REVOKED);
        repo.save(e);
        return BaseResponse.success("Tenant integration key revoked", null);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<TenantIntegrationKeyRes> get(final Long tikId) {
        TenantIntegrationKey e = repo.findByTikIdAndIsDeletedFalse(tikId)
                .orElseThrow(() -> new EntityNotFoundException("TenantIntegrationKey " + tikId));
        return BaseResponse.success("Tenant integration key fetched", mapper.toRes(e));
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<Page<TenantIntegrationKeyRes>> listByTenant(final Integer tenantId, final Pageable pageable) {
        // Validate tenant existence (optional but helpful)
        tenantRepo.findByIdAndIsDeletedFalse(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant " + tenantId));
        Page<TenantIntegrationKeyRes> page =
                repo.findByTenantIdAndIsDeletedFalse(tenantId, pageable).map(mapper::toRes);
        return BaseResponse.success("Tenant integration keys listed", page);
    }

    private String resolveActor(final String actor) {
        return StringUtils.isBlank(actor) ? DEFAULT_ACTOR : actor.trim();
    }

    private String generateSecret() {
        byte[] secretBytes = new byte[SECRET_BYTES_LENGTH];
        RANDOM.nextBytes(secretBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
    }

    private String validateSecret(final String secret) {
        if (StringUtils.isBlank(secret)) {
            throw new IllegalArgumentException("Secret must not be blank");
        }
        String trimmed = secret.trim();
        if (trimmed.length() < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException(
                    "Secret must be at least " + MIN_SECRET_LENGTH + " characters long");
        }
        if (trimmed.length() > TenantIntegrationKey.PLAIN_SECRET_LENGTH) {
            throw new IllegalArgumentException("Secret exceeds maximum length");
        }

        int categories = 0;
        if (trimmed.chars().anyMatch(Character::isLowerCase)) {
            categories++;
        }
        if (trimmed.chars().anyMatch(Character::isUpperCase)) {
            categories++;
        }
        if (trimmed.chars().anyMatch(Character::isDigit)) {
            categories++;
        }
        if (trimmed.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) {
            categories++;
        }
        if (categories < 3) {
            throw new IllegalArgumentException(
                    "Secret must include at least three character classes (upper, lower, digit, symbol)");
        }

        long distinct = trimmed.chars().distinct().count();
        if (distinct < MIN_DISTINCT_CHARS) {
            throw new IllegalArgumentException(
                    "Secret must contain at least " + MIN_DISTINCT_CHARS + " unique characters");
        }
        return trimmed;
    }
}