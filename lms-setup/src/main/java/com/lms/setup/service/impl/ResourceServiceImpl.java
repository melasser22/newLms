package com.lms.setup.service.impl;

import com.common.dto.BaseResponse;
import com.lms.setup.model.Resource;
import com.lms.setup.repository.ResourceRepository;
import com.lms.setup.service.ResourceService;
import com.common.sort.SortUtils;
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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class ResourceServiceImpl implements ResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceServiceImpl.class);

    private final ResourceRepository resourceRepository;

    public ResourceServiceImpl(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Override
    @Audited(action = AuditAction.CREATE, entity = "Resource", dataClass = DataClass.HEALTH, message = "Create resource")
    @CacheEvict(cacheNames = {"resources:children"}, allEntries = true)
    public BaseResponse<Resource> add(Resource request) {
        try {
            if (request.getResourceCd() == null || request.getResourceCd().isBlank()) {
                return BaseResponse.error("ERR_RESOURCE_CD_REQUIRED", "Resource code is required");
            }
            if (resourceRepository.existsByResourceCdIgnoreCase(request.getResourceCd())) {
                return BaseResponse.error("ERR_RESOURCE_DUP_CD", "Resource code already exists");
            }
            if (request.getPath() != null && request.getHttpMethod() != null &&
                resourceRepository.findByPathIgnoreCaseAndHttpMethodIgnoreCase(request.getPath(), request.getHttpMethod()).isPresent()) {
                return BaseResponse.error("ERR_RESOURCE_DUP_ENDPOINT", "A resource with same path+method exists");
            }
            Resource saved = resourceRepository.save(request);
            return BaseResponse.success("Resource created", saved);
        } catch (Exception ex) {
            LOGGER.error("Add resource failed", ex);
            return BaseResponse.error("ERR_RESOURCE_ADD", ex.getMessage());
        }
    }

    @Override
    @Audited(action = AuditAction.UPDATE, entity = "Resource", dataClass = DataClass.HEALTH, message = "Update resource")
    @CacheEvict(cacheNames = {"resources:children"}, allEntries = true)
    public BaseResponse<Resource> update(Integer resourceId, Resource request) {
        try {
            Resource existing = resourceRepository.findById(resourceId).orElse(null);
            if (existing == null) {
                return BaseResponse.error("ERR_RESOURCE_NOT_FOUND", "Resource not found");
            }

            if (request.getResourceCd() != null &&
                !request.getResourceCd().equalsIgnoreCase(existing.getResourceCd()) &&
                resourceRepository.existsByResourceCdIgnoreCase(request.getResourceCd())) {
                return BaseResponse.error("ERR_RESOURCE_DUP_CD", "Resource code already exists");
            }
            if (request.getPath() != null && request.getHttpMethod() != null) {
                resourceRepository.findByPathIgnoreCaseAndHttpMethodIgnoreCase(request.getPath(), request.getHttpMethod())
                        .filter(r -> !r.getResourceId().equals(existing.getResourceId()))
                        .ifPresent(r -> { throw new IllegalStateException("A resource with same path+method exists"); });
            }

            if (request.getResourceCd() != null) existing.setResourceCd(request.getResourceCd());
            if (request.getResourceEnNm() != null) existing.setResourceEnNm(request.getResourceEnNm());
            if (request.getResourceArNm() != null) existing.setResourceArNm(request.getResourceArNm());
            if (request.getPath() != null)        existing.setPath(request.getPath());
            if (request.getHttpMethod() != null)  existing.setHttpMethod(request.getHttpMethod());
            if (request.getParentResourceId() != null) existing.setParentResourceId(request.getParentResourceId());
            if (request.getIsActive() != null)    existing.setIsActive(request.getIsActive());

            Resource saved = resourceRepository.save(existing);
            return BaseResponse.success("Resource updated", saved);
        } catch (IllegalStateException ise) {
            return BaseResponse.error("ERR_RESOURCE_DUP_ENDPOINT", ise.getMessage());
        } catch (Exception ex) {
            LOGGER.error("Update resource failed", ex);
            return BaseResponse.error("ERR_RESOURCE_UPDATE", ex.getMessage());
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Resource", dataClass = DataClass.HEALTH, message = "Get resource")
    public BaseResponse<Resource> get(Integer resourceId) {
        try {
            return resourceRepository.findById(resourceId)
                    .map(r -> BaseResponse.success("Resource", r))
                    .orElseGet(() -> BaseResponse.error("ERR_RESOURCE_NOT_FOUND", "Resource not found"));
        } catch (Exception ex) {
            LOGGER.error("Get resource failed", ex);
            return BaseResponse.error("ERR_RESOURCE_GET", ex.getMessage());
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Resource", dataClass = DataClass.HEALTH, message = "List resources")
    public BaseResponse<?> list(Pageable pageable, String q, boolean all) {
        try {
            Sort sort = SortUtils.sanitize(pageable != null ? pageable.getSort() : Sort.unsorted(),
                    "resourceEnNm", "resourceEnNm", "resourceArNm", "resourceCd");
            Pageable pg = (pageable == null || !pageable.isPaged()
                    ? Pageable.unpaged()
                    : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort));
            if (all) {
                List<Resource> list;
                if (q == null || q.isBlank()) {
                    list = resourceRepository.findAll(sort);
                } else {
                    list = resourceRepository
                            .findByResourceEnNmContainingIgnoreCaseOrResourceArNmContainingIgnoreCase(q, q, PageRequest.of(0, Integer.MAX_VALUE, sort))
                            .getContent();
                }
                return BaseResponse.success("Resources list", list);
            }

            Page<Resource> page;
            if (q == null || q.isBlank()) {
                page = resourceRepository.findAll(pg);
            } else {
                page = resourceRepository.findByResourceEnNmContainingIgnoreCaseOrResourceArNmContainingIgnoreCase(q, q, pg);
            }
            return BaseResponse.success("Resources page", page);
        } catch (Exception ex) {
            LOGGER.error("List resources failed", ex);
            return BaseResponse.error("ERR_RESOURCE_LIST", ex.getMessage());
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Resource", dataClass = DataClass.HEALTH, message = "List active resources")
    public BaseResponse<List<Resource>> listActive() {
        try {
            List<Resource> list = resourceRepository
                    .findByIsActiveTrue(Pageable.unpaged())
                    .getContent();
            return BaseResponse.success("Active resources", list);
        } catch (Exception ex) {
            LOGGER.error("List active resources failed", ex);
            return BaseResponse.error("ERR_RESOURCE_LIST_ACTIVE", ex.getMessage());
        }
    }

    @Cacheable(cacheNames = "resources:children", key = "#parentResourceId")
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Resource> childrenRaw(Integer parentResourceId) {
        return resourceRepository.findByParentResourceIdOrderByResourceCdAsc(parentResourceId);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Resource", dataClass = DataClass.HEALTH, message = "Children of resource")
    public BaseResponse<List<Resource>> childrenOf(Integer parentResourceId) {
        try {
            return BaseResponse.success("Children", childrenRaw(parentResourceId));
        } catch (Exception ex) {
            LOGGER.error("List children resources failed", ex);
            return BaseResponse.error("ERR_RESOURCE_CHILDREN", ex.getMessage());
        }
    }
}
