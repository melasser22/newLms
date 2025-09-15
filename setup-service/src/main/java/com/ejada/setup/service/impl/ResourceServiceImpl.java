package com.ejada.setup.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.ResourceDto;
import com.ejada.setup.mapper.ResourceMapper;
import com.ejada.setup.model.Resource;
import com.ejada.setup.repository.ResourceRepository;
import com.ejada.setup.service.ResourceService;
import com.ejada.common.sort.SortUtils;
import com.ejada.audit.starter.api.AuditAction;
import com.ejada.audit.starter.api.DataClass;
import com.ejada.audit.starter.api.annotations.Audited;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class ResourceServiceImpl implements ResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceServiceImpl.class);
    private static final int LARGE_PAGE_SIZE = 1000;

    private final ResourceRepository resourceRepository;
    private final ResourceMapper mapper;

    public ResourceServiceImpl(final ResourceRepository resourceRepository, final ResourceMapper mapper) {
        this.resourceRepository = resourceRepository;
        this.mapper = mapper;
    }

    @Override
    @Audited(action = AuditAction.CREATE, entity = "Resource", dataClass = DataClass.HEALTH, message = "Create resource")
    @CacheEvict(cacheNames = {"resources:children"}, allEntries = true)
    public BaseResponse<ResourceDto> add(final ResourceDto request) {
        try {
            if (request.getResourceCd() == null || request.getResourceCd().isBlank()) {
                return BaseResponse.error("ERR_RESOURCE_CD_REQUIRED", "Resource code is required");
            }
            if (resourceRepository.existsByResourceCdIgnoreCase(request.getResourceCd())) {
                return BaseResponse.error("ERR_RESOURCE_DUP_CD", "Resource code already exists");
            }
            if (request.getPath() != null && request.getHttpMethod() != null
                    && resourceRepository
                            .findByPathIgnoreCaseAndHttpMethodIgnoreCase(request.getPath(), request.getHttpMethod())
                            .isPresent()) {
                return BaseResponse.error("ERR_RESOURCE_DUP_ENDPOINT", "A resource with same path+method exists");
            }
            Resource entity = mapper.toEntity(request);
            Resource saved = resourceRepository.save(entity);
            return BaseResponse.success("Resource created", mapper.toDto(saved));
        } catch (Exception ex) {
            LOGGER.error("Add resource failed", ex);
            return BaseResponse.error("ERR_RESOURCE_ADD", "Failed to create resource");
        }
    }

    @Override
    @Audited(action = AuditAction.UPDATE, entity = "Resource", dataClass = DataClass.HEALTH, message = "Update resource")
    @CacheEvict(cacheNames = {"resources:children"}, allEntries = true)
    public BaseResponse<ResourceDto> update(final Integer resourceId, final ResourceDto request) {
        try {
            Resource existing = resourceRepository.findById(resourceId).orElse(null);
            if (existing == null) {
                return BaseResponse.error("ERR_RESOURCE_NOT_FOUND", "Resource not found");
            }

            if (request.getResourceCd() != null
                    && !request.getResourceCd().equalsIgnoreCase(existing.getResourceCd())
                    && resourceRepository.existsByResourceCdIgnoreCase(request.getResourceCd())) {
                return BaseResponse.error("ERR_RESOURCE_DUP_CD", "Resource code already exists");
            }
            if (request.getPath() != null && request.getHttpMethod() != null) {
                resourceRepository.findByPathIgnoreCaseAndHttpMethodIgnoreCase(request.getPath(), request.getHttpMethod())
                        .filter(r -> !r.getResourceId().equals(existing.getResourceId()))
                        .ifPresent(r -> {
                            throw new IllegalStateException("A resource with same path+method exists");
                        });
            }

            if (request.getResourceCd() != null) {
                existing.setResourceCd(request.getResourceCd());
            }
            if (request.getResourceEnNm() != null) {
                existing.setResourceEnNm(request.getResourceEnNm());
            }
            if (request.getResourceArNm() != null) {
                existing.setResourceArNm(request.getResourceArNm());
            }
            if (request.getPath() != null) {
                existing.setPath(request.getPath());
            }
            if (request.getHttpMethod() != null) {
                existing.setHttpMethod(request.getHttpMethod());
            }
            if (request.getParentResourceId() != null) {
                existing.setParentResourceId(request.getParentResourceId());
            }
            if (request.getIsActive() != null) {
                existing.setIsActive(request.getIsActive());
            }
            if (request.getEnDescription() != null) {
                existing.setEnDescription(request.getEnDescription());
            }
            if (request.getArDescription() != null) {
                existing.setArDescription(request.getArDescription());
            }

            Resource saved = resourceRepository.save(existing);
            return BaseResponse.success("Resource updated", mapper.toDto(saved));
        } catch (IllegalStateException ise) {
            return BaseResponse.error("ERR_RESOURCE_DUP_ENDPOINT", ise.getMessage());
        } catch (Exception ex) {
            LOGGER.error("Update resource failed", ex);
            return BaseResponse.error("ERR_RESOURCE_UPDATE", "Failed to update resource");
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Resource", dataClass = DataClass.HEALTH, message = "Get resource")
    public BaseResponse<ResourceDto> get(final Integer resourceId) {
        try {
            return resourceRepository.findById(resourceId)
                    .map(r -> BaseResponse.success("Resource", mapper.toDto(r)))
                    .orElseGet(() -> BaseResponse.error("ERR_RESOURCE_NOT_FOUND", "Resource not found"));
        } catch (Exception ex) {
            LOGGER.error("Get resource failed", ex);
            return BaseResponse.error("ERR_RESOURCE_GET", "Failed to get resource");
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Resource", dataClass = DataClass.HEALTH, message = "List resources")
    public BaseResponse<Page<ResourceDto>> list(final Pageable pageable, final String q, final boolean unpaged) {
        try {
            Sort sort = SortUtils.sanitize(pageable != null ? pageable.getSort() : Sort.unsorted(),
                    "resourceEnNm", "resourceArNm", "resourceCd");
            Pageable pg = (pageable == null || !pageable.isPaged()
                    ? Pageable.unpaged()
                    : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort));
            if (unpaged) {
                List<Resource> list;
                if (q == null || q.isBlank()) {
                    list = resourceRepository.findAll(sort);
                } else {
                    list = resourceRepository
                            .findByResourceEnNmContainingIgnoreCaseOrResourceArNmContainingIgnoreCase(
                                    q, q, PageRequest.of(0, LARGE_PAGE_SIZE, sort))
                            .getContent();
                }
                return BaseResponse.success("Resources list", new PageImpl<>(mapper.toDtoList(list)));
            }

            Page<Resource> page;
            if (q == null || q.isBlank()) {
                page = resourceRepository.findAll(pg);
            } else {
                page = resourceRepository.findByResourceEnNmContainingIgnoreCaseOrResourceArNmContainingIgnoreCase(q, q, pg);
            }
            return BaseResponse.success("Resources page", mapper.toDtoPage(page));
        } catch (Exception ex) {
            LOGGER.error("List resources failed", ex);
            return BaseResponse.error("ERR_RESOURCE_LIST", "Failed to list resources");
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Resource", dataClass = DataClass.HEALTH, message = "List active resources")
    public BaseResponse<List<ResourceDto>> listActive() {
        try {
            List<Resource> list = resourceRepository
                    .findByIsActiveTrue(Pageable.unpaged())
                    .getContent();
            return BaseResponse.success("Active resources", mapper.toDtoList(list));
        } catch (Exception ex) {
            LOGGER.error("List active resources failed", ex);
            return BaseResponse.error("ERR_RESOURCE_LIST_ACTIVE", "Failed to list active resources");
        }
    }

    @Cacheable(cacheNames = "resources:children", key = "#parentResourceId")
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Resource> childrenRaw(final Integer parentResourceId) {
        return resourceRepository.findByParentResourceIdOrderByResourceCdAsc(parentResourceId);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Resource", dataClass = DataClass.HEALTH, message = "Children of resource")
    public BaseResponse<List<ResourceDto>> childrenOf(final Integer parentResourceId) {
        try {
            return BaseResponse.success("Children", mapper.toDtoList(childrenRaw(parentResourceId)));
        } catch (Exception ex) {
            LOGGER.error("List children resources failed", ex);
            return BaseResponse.error("ERR_RESOURCE_CHILDREN", "Failed to list children resources");
        }
    }
}
