package com.ejada.setup.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.exception.DuplicateResourceException;
import com.ejada.common.exception.NotFoundException;
import com.ejada.common.service.BaseCrudService;
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
public class SystemParameterServiceImpl
        extends BaseCrudService<SystemParameter, Integer, SystemParameter, SystemParameter, SystemParameter>
        implements SystemParameterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemParameterServiceImpl.class);

    private final SystemParameterRepository systemParameterRepository;

    public SystemParameterServiceImpl(final SystemParameterRepository systemParameterRepository) {
        this.systemParameterRepository = systemParameterRepository;
    }

    @Override
    protected SystemParameterRepository getRepository() {
        return systemParameterRepository;
    }

    @Override
    protected boolean existsByUniqueField(final SystemParameter request) {
        return request != null
                && request.getParamKey() != null
                && systemParameterRepository.existsByParamKeyIgnoreCase(request.getParamKey());
    }

    @Override
    protected SystemParameter mapToEntity(final SystemParameter dto) {
        return dto;
    }

    @Override
    protected void updateEntity(final SystemParameter entity, final SystemParameter request) {
        if (request.getParamKey() != null
                && !request.getParamKey().equalsIgnoreCase(entity.getParamKey())
                && systemParameterRepository.existsByParamKeyIgnoreCase(request.getParamKey())) {
            throw new DuplicateResourceException("Parameter key already exists");
        }
        if (request.getParamKey() != null) {
            entity.setParamKey(request.getParamKey());
        }
        if (request.getParamValue() != null) {
            entity.setParamValue(request.getParamValue());
        }
        if (request.getParamGroup() != null) {
            entity.setParamGroup(request.getParamGroup());
        }
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
    }

    @Override
    protected SystemParameter mapToDto(final SystemParameter entity) {
        return entity;
    }

    @Override
    protected String getEntityName() {
        return "System parameter";
    }

    @Override
    protected String duplicateResourceMessage(final SystemParameter dto) {
        return "Parameter key already exists";
    }

    @Override
    @Audited(action = AuditAction.CREATE, entity = "SystemParameter", dataClass = DataClass.HEALTH, message = "Create system parameter")
    @CacheEvict(cacheNames = {"sysparams:byKeys"}, allEntries = true)
    public BaseResponse<SystemParameter> add(final SystemParameter request) {
        try {
            if (request.getParamKey() == null || request.getParamKey().isBlank()) {
                return BaseResponse.error("ERR_PARAM_KEY_REQUIRED", "Parameter key is required");
            }
            return super.create(request);
        } catch (DuplicateResourceException ex) {
            return BaseResponse.error("ERR_PARAM_DUP_KEY", ex.getMessage());
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
            return super.update(paramId, request);
        } catch (DuplicateResourceException ex) {
            return BaseResponse.error("ERR_PARAM_DUP_KEY", ex.getMessage());
        } catch (NotFoundException ex) {
            return BaseResponse.error("ERR_PARAM_NOT_FOUND", ex.getMessage());
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
            return super.get(paramId);
        } catch (NotFoundException ex) {
            return BaseResponse.error("ERR_PARAM_NOT_FOUND", ex.getMessage());
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
