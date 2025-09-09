package com.ejada.setup.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.model.SystemParameter;
import com.ejada.setup.repository.SystemParameterRepository;
import com.ejada.setup.service.SystemParameterService;
import com.ejada.audit.starter.api.AuditAction;
import com.ejada.audit.starter.api.DataClass;
import com.ejada.audit.starter.api.annotations.Audited;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ejada.common.sort.SortUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@Transactional
public class SystemParameterServiceImpl implements SystemParameterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemParameterServiceImpl.class);

    private final SystemParameterRepository systemParameterRepository;

    public SystemParameterServiceImpl(final SystemParameterRepository systemParameterRepository) {
        this.systemParameterRepository = systemParameterRepository;
    }

    @Override
    @Audited(action = AuditAction.CREATE, entity = "SystemParameter", dataClass = DataClass.HEALTH, message = "Create system parameter")
    @CacheEvict(cacheNames = {"sysparams:byKeys"}, allEntries = true)
    public BaseResponse<SystemParameter> add(final SystemParameter request) {
        try {
            if (request.getParamKey() == null || request.getParamKey().isBlank()) {
                return BaseResponse.error("ERR_PARAM_KEY_REQUIRED", "Parameter key is required");
            }
            if (systemParameterRepository.existsByParamKeyIgnoreCase(request.getParamKey())) {
                return BaseResponse.error("ERR_PARAM_DUP_KEY", "Parameter key already exists");
            }
            SystemParameter saved = systemParameterRepository.save(request);
            return BaseResponse.success("System parameter created", saved);
        } catch (Exception ex) {
            LOGGER.error("Add system parameter failed", ex);
            return BaseResponse.error("ERR_PARAM_ADD", "Failed to create system parameter");
        }
    }

    @Override
    @Audited(action = AuditAction.UPDATE, entity = "SystemParameter", dataClass = DataClass.HEALTH, message = "Update system parameter")
    @CacheEvict(cacheNames = {"sysparams:byKeys"}, allEntries = true)
    public BaseResponse<SystemParameter> update(final Integer paramId, final SystemParameter request) {
        try {
            SystemParameter existing = systemParameterRepository.findById(paramId).orElse(null);
            if (existing == null) {
                return BaseResponse.error("ERR_PARAM_NOT_FOUND", "System parameter not found");
            }

            if (request.getParamKey() != null
                    && !request.getParamKey().equalsIgnoreCase(existing.getParamKey())
                    && systemParameterRepository.existsByParamKeyIgnoreCase(request.getParamKey())) {
                return BaseResponse.error("ERR_PARAM_DUP_KEY", "Parameter key already exists");
            }
            if (request.getParamKey() != null) {
                existing.setParamKey(request.getParamKey());
            }
            if (request.getParamValue() != null) {
                existing.setParamValue(request.getParamValue());
            }
            if (request.getParamGroup() != null) {
                existing.setParamGroup(request.getParamGroup());
            }
            if (request.getIsActive() != null) {
                existing.setIsActive(request.getIsActive());
            }
            if (request.getDescription() != null) {
                existing.setDescription(request.getDescription());
            }

            SystemParameter saved = systemParameterRepository.save(existing);
            return BaseResponse.success("System parameter updated", saved);
        } catch (Exception ex) {
            LOGGER.error("Update system parameter failed", ex);
            return BaseResponse.error("ERR_PARAM_UPDATE", "Failed to update system parameter");
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "SystemParameter", dataClass = DataClass.HEALTH, message = "Get system parameter")
    public BaseResponse<SystemParameter> get(final Integer paramId) {
        try {
            return systemParameterRepository.findById(paramId)
                    .map(p -> BaseResponse.success("System parameter", p))
                    .orElseGet(() -> BaseResponse.error("ERR_PARAM_NOT_FOUND", "System parameter not found"));
        } catch (Exception ex) {
            LOGGER.error("Get system parameter failed", ex);
            return BaseResponse.error("ERR_PARAM_GET", "Failed to get system parameter");
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "SystemParameter", dataClass = DataClass.HEALTH, message = "List system parameters")
    public BaseResponse<Page<SystemParameter>> list(final Pageable pageable, final String group, final Boolean onlyActive) {
        try {
            Sort sort = SortUtils.sanitize(pageable != null ? pageable.getSort() : Sort.unsorted(),
                    "paramKey", "paramGroup", "paramValue");
            Pageable pg = (pageable == null || !pageable.isPaged()
                    ? Pageable.unpaged()
                    : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort));

            Page<SystemParameter> page;
            if (Boolean.TRUE.equals(onlyActive)) {
                page = systemParameterRepository.findByIsActiveTrue(pg);
            } else if (group != null && !group.isBlank()) {
                page = systemParameterRepository.findByParamGroupIgnoreCase(group, pg);
            } else {
                page = systemParameterRepository.findAll(pg);
            }
            return BaseResponse.success("System parameters page", page);
        } catch (Exception ex) {
            LOGGER.error("List system parameters failed", ex);
            return BaseResponse.error("ERR_PARAM_LIST", "Failed to list system parameters");
        }
    }

    @Cacheable(cacheNames = "sysparams:byKeys", key = "#keys")
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<SystemParameter> getByKeysRaw(final Collection<String> keys) {
        return systemParameterRepository.findByParamKeyIn(keys);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "SystemParameter", dataClass = DataClass.HEALTH, message = "Get system parameters by keys")
    public BaseResponse<List<SystemParameter>> getByKeys(final List<String> keys) {
        try {
            if (keys == null || keys.isEmpty()) {
                return BaseResponse.error("ERR_PARAM_KEYS_REQUIRED", "Keys list is required");
            }
            return BaseResponse.success("Parameters", getByKeysRaw(keys));
        } catch (Exception ex) {
            LOGGER.error("Get system parameters by keys failed", ex);
            return BaseResponse.error("ERR_PARAM_KEYS", "Failed to get system parameters by keys");
        }
    }
    
    @Override
    public BaseResponse<SystemParameter> getByKey(final String paramKey) {
        return systemParameterRepository.findByParamKey(paramKey)
                .map(p -> BaseResponse.success("System parameter", p))
                .orElseGet(() -> BaseResponse.error("ERR_PARAM_NOT_FOUND", "System parameter not found"));
    }
}
