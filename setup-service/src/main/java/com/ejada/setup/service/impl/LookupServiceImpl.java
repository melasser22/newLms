package com.ejada.setup.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.model.Lookup;
import com.ejada.setup.repository.LookupRepository;
import com.ejada.setup.service.LookupService;
import com.ejada.audit.starter.api.AuditAction;
import com.ejada.audit.starter.api.DataClass;
import com.ejada.audit.starter.api.annotations.Audited;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public final class LookupServiceImpl implements LookupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LookupServiceImpl.class);

    private final LookupRepository lookupRepository;

    public LookupServiceImpl(final LookupRepository lookupRepository) {
        this.lookupRepository = lookupRepository;
    }

    /**
     * IMPORTANT:
     * Cache ONLY raw lists to avoid BaseResponse being deserialized as LinkedHashMap
     * when using JSON Redis serializers (root cause of ClassCastException you saw).
     */
    @Cacheable(cacheNames = "lookups:all", key = "'all'")
    public List<Lookup> getAllRaw() {
        return lookupRepository.findAll();
    }

    @Override
    @Audited(action = AuditAction.READ, entity = "Lookup", dataClass = DataClass.HEALTH, message = "Fetch all lookups")
    public BaseResponse<List<Lookup>> getAll() {
        try {
            return BaseResponse.success("Fetched all lookups", getAllRaw());
        } catch (Exception ex) {
            LOGGER.error("Exception while retrieving lookups", ex);
            return BaseResponse.error("ERR_LOOKUP_ALL", "Failed to fetch lookups");
        }
    }

    @Cacheable(cacheNames = "lookups:byGroup", key = "#groupCode")
    public List<Lookup> getByGroupRaw(final String groupCode) {
        return lookupRepository.findByLookupGroupCodeAndIsActiveTrueOrderByLookupItemEnNmAsc(groupCode);
    }

    @Override
    @Audited(action = AuditAction.READ, entity = "Lookup", dataClass = DataClass.HEALTH, message = "Fetch lookups by group")
    public BaseResponse<List<Lookup>> getByGroup(final String groupCode) {
        try {
            if (groupCode == null || groupCode.isBlank()) {
                return BaseResponse.error("ERR_LOOKUP_GROUP_REQUIRED", "Group code is required");
            }
            return BaseResponse.success("Fetched lookups by group", getByGroupRaw(groupCode));
        } catch (Exception ex) {
            LOGGER.error("Exception while retrieving lookups by group", ex);
            return BaseResponse.error("ERR_LOOKUP_GROUP", "Failed to fetch lookups by group");
        }
    }

    @Override
    @Audited(action = AuditAction.CREATE, entity = "Lookup", dataClass = DataClass.HEALTH, message = "Create lookup")
    @CacheEvict(cacheNames = {"lookups:all", "lookups:byGroup"}, allEntries = true)
    public BaseResponse<Lookup> add(final Lookup request) {
        try {
            if (request.getLookupItemId() == null) {
                return BaseResponse.error("ERR_LOOKUP_ID_REQUIRED", "Lookup item id is required");
            }
            if (request.getLookupItemCd() == null || request.getLookupItemCd().isBlank()) {
                return BaseResponse.error("ERR_LOOKUP_CD_REQUIRED", "Lookup item code is required");
            }
            if (lookupRepository.existsById(request.getLookupItemId())) {
                return BaseResponse.error("ERR_LOOKUP_DUP_ID", "Lookup item id already exists");
            }
            Lookup saved = lookupRepository.save(request);
            return BaseResponse.success("Lookup created", saved);
        } catch (Exception ex) {
            LOGGER.error("Add lookup failed", ex);
            return BaseResponse.error("ERR_LOOKUP_ADD", "Failed to create lookup");
        }
    }

    @Override
    @Audited(action = AuditAction.UPDATE, entity = "Lookup", dataClass = DataClass.HEALTH, message = "Update lookup")
    @CacheEvict(cacheNames = {"lookups:all", "lookups:byGroup"}, allEntries = true)
    public BaseResponse<Lookup> update(final Integer lookupItemId, final Lookup request) {
        try {
            Lookup existing = lookupRepository.findById(lookupItemId).orElse(null);
            if (existing == null) {
                return BaseResponse.error("ERR_LOOKUP_NOT_FOUND", "Lookup not found");
            }

            if (request.getLookupItemCd() != null) {
                existing.setLookupItemCd(request.getLookupItemCd());
            }
            if (request.getLookupItemEnNm() != null) {
                existing.setLookupItemEnNm(request.getLookupItemEnNm());
            }
            if (request.getLookupItemArNm() != null) {
                existing.setLookupItemArNm(request.getLookupItemArNm());
            }
            if (request.getLookupGroupCode() != null) {
                existing.setLookupGroupCode(request.getLookupGroupCode());
            }
            if (request.getParentLookupId() != null) {
                existing.setParentLookupId(request.getParentLookupId());
            }
            if (request.getIsActive() != null) {
                existing.setIsActive(request.getIsActive());
            }
            if (request.getItemEnDescription() != null) {
                existing.setItemEnDescription(request.getItemEnDescription());
            }
            if (request.getItemArDescription() != null) {
                existing.setItemArDescription(request.getItemArDescription());
            }

            Lookup saved = lookupRepository.save(existing);
            return BaseResponse.success("Lookup updated", saved);
        } catch (Exception ex) {
            LOGGER.error("Update lookup failed", ex);
            return BaseResponse.error("ERR_LOOKUP_UPDATE", "Failed to update lookup");
        }
    }
    
    @Override
    public BaseResponse<List<Lookup>> getAllLookups() {
        return getAll();
    }
}
