package com.lms.setup.service.impl;

import com.common.dto.BaseResponse;
import com.lms.setup.model.SystemParameter;
import com.lms.setup.repository.SystemParameterRepository;
import com.lms.setup.service.SystemParameterService;
import com.shared.audit.starter.api.AuditAction;
import com.shared.audit.starter.api.DataClass;
import com.shared.audit.starter.api.annotations.Audited;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.common.sort.SortUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SystemParameterServiceImpl implements SystemParameterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemParameterServiceImpl.class);

    private final SystemParameterRepository systemParameterRepository;

    public SystemParameterServiceImpl(SystemParameterRepository systemParameterRepository) {
        this.systemParameterRepository = systemParameterRepository;
    }

    @Override
    @Audited(action = AuditAction.CREATE, entity = "SystemParameter", dataClass = DataClass.HEALTH, message = "Create system parameter")
    @CacheEvict(cacheNames = {"sysparams:byKeys"}, allEntries = true)
    public BaseResponse<SystemParameter> add(SystemParameter request) {
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
            return BaseResponse.error("ERR_PARAM_ADD", ex.getMessage());
        }
    }

    @Override
    @Audited(action = AuditAction.UPDATE, entity = "SystemParameter", dataClass = DataClass.HEALTH, message = "Update system parameter")
    @CacheEvict(cacheNames = {"sysparams:byKeys"}, allEntries = true)
    public BaseResponse<SystemParameter> update(Integer paramId, SystemParameter request) {
        try {
            SystemParameter existing = systemParameterRepository.findById(paramId).orElse(null);
            if (existing == null) {
                return BaseResponse.error("ERR_PARAM_NOT_FOUND", "System parameter not found");
            }

            if (request.getParamKey() != null &&
                !request.getParamKey().equalsIgnoreCase(existing.getParamKey()) &&
                systemParameterRepository.existsByParamKeyIgnoreCase(request.getParamKey())) {
                return BaseResponse.error("ERR_PARAM_DUP_KEY", "Parameter key already exists");
            }

            if (request.getParamKey() != null)   existing.setParamKey(request.getParamKey());
            if (request.getParamValue() != null) existing.setParamValue(request.getParamValue());
            if (request.getParamGroup() != null) existing.setParamGroup(request.getParamGroup());
            if (request.getIsActive() != null)   existing.setIsActive(request.getIsActive());
            if (request.getDescription() != null) existing.setDescription(request.getDescription());

            SystemParameter saved = systemParameterRepository.save(existing);
            return BaseResponse.success("System parameter updated", saved);
        } catch (Exception ex) {
            LOGGER.error("Update system parameter failed", ex);
            return BaseResponse.error("ERR_PARAM_UPDATE", ex.getMessage());
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "SystemParameter", dataClass = DataClass.HEALTH, message = "Get system parameter")
    public BaseResponse<SystemParameter> get(Integer paramId) {
        try {
            return systemParameterRepository.findById(paramId)
                    .map(p -> BaseResponse.success("System parameter", p))
                    .orElseGet(() -> BaseResponse.error("ERR_PARAM_NOT_FOUND", "System parameter not found"));
        } catch (Exception ex) {
            LOGGER.error("Get system parameter failed", ex);
            return BaseResponse.error("ERR_PARAM_GET", ex.getMessage());
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "SystemParameter", dataClass = DataClass.HEALTH, message = "List system parameters")
    public BaseResponse<Page<SystemParameter>> list(Pageable pageable, String group, Boolean onlyActive) {
        try {
            Sort sort = SortUtils.sanitize(pageable != null ? pageable.getSort() : Sort.unsorted(),
                    "paramKey", "paramKey", "paramGroup", "paramValue");
            Pageable pg = (pageable == null ? Pageable.unpaged()
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
            return BaseResponse.error("ERR_PARAM_LIST", ex.getMessage());
        }
    }

    @Cacheable(cacheNames = "sysparams:byKeys", key = "#keys")
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<SystemParameter> getByKeysRaw(Collection<String> keys) {
        return systemParameterRepository.findByParamKeyIn(keys);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "SystemParameter", dataClass = DataClass.HEALTH, message = "Get system parameters by keys")
    public BaseResponse<List<SystemParameter>> getByKeys(List<String> keys) {
        try {
            if (keys == null || keys.isEmpty()) {
                return BaseResponse.error("ERR_PARAM_KEYS_REQUIRED", "Keys list is required");
            }
            return BaseResponse.success("Parameters", getByKeysRaw(keys));
        } catch (Exception ex) {
            LOGGER.error("Get system parameters by keys failed", ex);
            return BaseResponse.error("ERR_PARAM_KEYS", ex.getMessage());
        }
    }
    
    @Override
    public BaseResponse<SystemParameter> getByKey(String paramKey) {
        Optional<SystemParameter> opt = systemParameterRepository.findByParamKey(paramKey);
        BaseResponse<SystemParameter> resp = new BaseResponse<>();
        resp.success(opt.isPresent());
        resp.setData(opt.orElse(null));
        return resp;
    }
}
