package com.ejada.setup.service.impl;

import com.ejada.common.cache.CacheEvictionUtils;
import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.LookupCreateRequest;
import com.ejada.setup.dto.LookupResponse;
import com.ejada.setup.dto.LookupUpdateRequest;
import com.ejada.setup.model.Lookup;
import com.ejada.setup.repository.LookupRepository;
import com.ejada.setup.service.LookupService;
import com.ejada.audit.starter.api.AuditAction;
import com.ejada.audit.starter.api.DataClass;
import com.ejada.audit.starter.api.annotations.Audited;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Transactional
public class LookupServiceImpl implements LookupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LookupServiceImpl.class);

    private static final String CACHE_ALL_LOOKUPS = "lookups:all";
    private static final String CACHE_ALL_KEY = "all";
    private static final String CACHE_LOOKUPS_BY_GROUP = "lookups:byGroup";

    private final LookupRepository lookupRepository;
    private final CacheManager cacheManager;

    public LookupServiceImpl(final LookupRepository lookupRepository, final CacheManager cacheManager) {
        this.lookupRepository = lookupRepository;
        this.cacheManager = cacheManager;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    List<Lookup> getAllRaw() {
        return fetchCached(CACHE_ALL_LOOKUPS, CACHE_ALL_KEY, lookupRepository::findAll);
    }

    @Override
    @Audited(action = AuditAction.READ, entity = "Lookup", dataClass = DataClass.HEALTH, message = "Fetch all lookups")
    public BaseResponse<List<LookupResponse>> getAll() {
        try {
            List<LookupResponse> responses = mapToResponses(getAllRaw());
            return BaseResponse.success("Fetched all lookups", responses);
        } catch (Exception ex) {
            LOGGER.error("Exception while retrieving lookups", ex);
            return BaseResponse.error("ERR_LOOKUP_ALL", "Failed to fetch lookups");
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    List<Lookup> getByGroupRaw(final String groupCode) {
        return fetchCached(
                CACHE_LOOKUPS_BY_GROUP,
                groupCode,
                () -> lookupRepository.findByLookupGroupCodeAndIsActiveTrueOrderByLookupItemEnNmAsc(groupCode));
    }

    @Override
    @Audited(action = AuditAction.READ, entity = "Lookup", dataClass = DataClass.HEALTH, message = "Fetch lookups by group")
    public BaseResponse<List<LookupResponse>> getByGroup(final String groupCode) {
        try {
            if (StringUtils.isBlank(groupCode)) {
                return BaseResponse.error("ERR_LOOKUP_GROUP_REQUIRED", "Group code is required");
            }
            List<LookupResponse> responses = mapToResponses(getByGroupRaw(groupCode));
            return BaseResponse.success("Fetched lookups by group", responses);
        } catch (Exception ex) {
            LOGGER.error("Exception while retrieving lookups by group", ex);
            return BaseResponse.error("ERR_LOOKUP_GROUP", "Failed to fetch lookups by group");
        }
    }

    @Override
    @Audited(action = AuditAction.CREATE, entity = "Lookup", dataClass = DataClass.HEALTH, message = "Create lookup")
    public BaseResponse<LookupResponse> add(final LookupCreateRequest request) {
        try {
            Lookup toSave = mapCreate(request);
            Lookup saved = lookupRepository.save(toSave);
            evictCaches(saved.getLookupGroupCode());
            return BaseResponse.success("Lookup created", toResponse(saved));
        } catch (DataIntegrityViolationException ex) {
            LOGGER.warn("Lookup creation failed because of duplicate key: {}", ex.getMessage());
            return BaseResponse.error("ERR_LOOKUP_DUPLICATE", "Lookup already exists");
        } catch (Exception ex) {
            LOGGER.error("Add lookup failed", ex);
            return BaseResponse.error("ERR_LOOKUP_ADD", "Failed to create lookup");
        }
    }

    @Override
    @Audited(action = AuditAction.UPDATE, entity = "Lookup", dataClass = DataClass.HEALTH, message = "Update lookup")
    public BaseResponse<LookupResponse> update(final Integer lookupItemId, final LookupUpdateRequest request) {
        try {
            Lookup existing = lookupRepository.findById(lookupItemId).orElse(null);
            if (existing == null) {
                return BaseResponse.error("ERR_LOOKUP_NOT_FOUND", "Lookup not found");
            }

            String previousGroup = existing.getLookupGroupCode();
            applyUpdate(existing, request);

            Lookup saved = lookupRepository.save(existing);
            evictCaches(previousGroup, saved.getLookupGroupCode());
            return BaseResponse.success("Lookup updated", toResponse(saved));
        } catch (DataIntegrityViolationException ex) {
            LOGGER.warn("Lookup update failed because of duplicate key: {}", ex.getMessage());
            return BaseResponse.error("ERR_LOOKUP_DUPLICATE", "Lookup already exists");
        } catch (Exception ex) {
            LOGGER.error("Update lookup failed", ex);
            return BaseResponse.error("ERR_LOOKUP_UPDATE", "Failed to update lookup");
        }
    }

    @Override
    public BaseResponse<List<LookupResponse>> getAllLookups() {
        return getAll();
    }

    private Lookup mapCreate(final LookupCreateRequest request) {
        return Lookup.builder()
                .lookupItemId(request.lookupItemId())
                .lookupItemCd(request.lookupItemCd())
                .lookupItemEnNm(request.lookupItemEnNm())
                .lookupItemArNm(request.lookupItemArNm())
                .lookupGroupCode(request.lookupGroupCode())
                .parentLookupId(request.parentLookupId())
                .isActive(request.isActive())
                .itemEnDescription(request.itemEnDescription())
                .itemArDescription(request.itemArDescription())
                .build();
    }

    private void applyUpdate(final Lookup existing, final LookupUpdateRequest request) {
        if (request.lookupItemCd() != null) {
            existing.setLookupItemCd(request.lookupItemCd());
        }
        if (request.lookupItemEnNm() != null) {
            existing.setLookupItemEnNm(request.lookupItemEnNm());
        }
        if (request.lookupItemArNm() != null) {
            existing.setLookupItemArNm(request.lookupItemArNm());
        }
        if (request.lookupGroupCode() != null) {
            existing.setLookupGroupCode(request.lookupGroupCode());
        }
        if (request.parentLookupId() != null) {
            existing.setParentLookupId(request.parentLookupId());
        }
        if (request.isActive() != null) {
            existing.setIsActive(request.isActive());
        }
        if (request.itemEnDescription() != null) {
            existing.setItemEnDescription(request.itemEnDescription());
        }
        if (request.itemArDescription() != null) {
            existing.setItemArDescription(request.itemArDescription());
        }
    }

    private List<LookupResponse> mapToResponses(final List<Lookup> lookups) {
        if (lookups == null || lookups.isEmpty()) {
            return List.of();
        }
        return lookups.stream().filter(Objects::nonNull)
                .map(this::toResponse)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private LookupResponse toResponse(final Lookup lookup) {
        if (lookup == null) {
            return null;
        }
        return new LookupResponse(
                lookup.getLookupItemId(),
                lookup.getLookupItemCd(),
                lookup.getLookupItemEnNm(),
                lookup.getLookupItemArNm(),
                lookup.getLookupGroupCode(),
                lookup.getParentLookupId(),
                lookup.getIsActive(),
                lookup.getItemEnDescription(),
                lookup.getItemArDescription()
        );
    }

    private void evictCaches(@Nullable final String... groupCodes) {
        CacheEvictionUtils.evict(cacheManager, CACHE_ALL_LOOKUPS, CACHE_ALL_KEY);
        if (groupCodes != null) {
            CacheEvictionUtils.evict(cacheManager, CACHE_LOOKUPS_BY_GROUP, (Object[]) groupCodes);
        }
    }

    /**
     * Cache ONLY raw entity lists. Caching {@link BaseResponse} objects causes serialization issues
     * when Redis (JSON) is used because responses are restored as {@code LinkedHashMap} instances.
     */
    private List<Lookup> fetchCached(final String cacheName, final Object key, final Supplier<List<Lookup>> loader) {
        Cache cache = cacheManager != null ? cacheManager.getCache(cacheName) : null;
        if (cache == null || key == null) {
            return toImmutableList(loader.get());
        }
        try {
            return cache.get(key, () -> toImmutableList(loader.get()));
        } catch (Cache.ValueRetrievalException ex) {
            LOGGER.warn("Cache retrieval failed for '{}' and key '{}'. Falling back to repository.", cacheName, key, ex);
            return toImmutableList(loader.get());
        }
    }

    private List<Lookup> toImmutableList(final List<Lookup> lookups) {
        if (lookups == null || lookups.isEmpty()) {
            return List.of();
        }
        return List.copyOf(lookups);
    }
}
