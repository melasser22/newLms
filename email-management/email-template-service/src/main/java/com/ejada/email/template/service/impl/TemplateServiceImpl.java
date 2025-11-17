package com.ejada.email.template.service.impl;

import com.ejada.common.context.ContextManager;
import com.ejada.email.template.domain.entity.TemplateEntity;
import com.ejada.email.template.domain.entity.TemplateVersionEntity;
import com.ejada.email.template.domain.enums.TemplateVersionStatus;
import com.ejada.email.template.domain.value.AttachmentMetadata;
import com.ejada.email.template.dto.AttachmentMetadataDto;
import com.ejada.email.template.dto.CreateTemplateRequest;
import com.ejada.email.template.dto.TemplateCloneRequest;
import com.ejada.email.template.dto.TemplateDto;
import com.ejada.email.template.dto.TemplatePreviewRequest;
import com.ejada.email.template.dto.TemplatePreviewResponse;
import com.ejada.email.template.dto.TemplateValidationRequest;
import com.ejada.email.template.dto.TemplateValidationResponse;
import com.ejada.email.template.dto.TemplateVersionCreateRequest;
import com.ejada.email.template.dto.TemplateVersionDto;
import com.ejada.email.template.dto.UpdateTemplateRequest;
import com.ejada.email.template.exception.TemplateNotFoundException;
import com.ejada.email.template.exception.TemplateVersionNotFoundException;
import com.ejada.email.template.mapper.TemplateMapper;
import com.ejada.email.template.repository.TemplateRepository;
import com.ejada.email.template.repository.TemplateVersionRepository;
import com.ejada.email.template.service.TemplateService;
import com.ejada.email.template.service.support.SendGridTemplateClient;
import com.ejada.email.template.service.support.TemplateRenderer;
import com.ejada.email.template.service.support.TemplateValidator;
import com.ejada.common.exception.ValidationException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateServiceImpl implements TemplateService {

  private static final Set<String> ALLOWED_SORT_PROPERTIES =
      Set.of("id", "name", "locale", "description", "archived", "createdAt", "updatedAt");

  private final TemplateRepository templateRepository;
  private final TemplateVersionRepository versionRepository;
  private final TemplateMapper templateMapper;
  private final TemplateRenderer templateRenderer;
  private final TemplateValidator templateValidator;
  private final SendGridTemplateClient sendGridTemplateClient;

  @Override
  public TemplateDto createTemplate(CreateTemplateRequest request) {
    requireTenantId();
    TemplateEntity entity = new TemplateEntity();
    entity.setName(request.getName());
    entity.setLocale(request.getLocale());
    entity.setDescription(request.getDescription());
    entity.setMetadata(request.getMetadata());
    entity.setDefaultAttachments(convertAttachments(request.getDefaultAttachments()));
    TemplateEntity saved = templateRepository.save(entity);
    return templateMapper.toDto(saved);
  }

  @Override
  @CacheEvict(value = {"templates", "activeTemplateVersions"}, key = "#templateId")
  public TemplateDto updateTemplate(Long templateId, UpdateTemplateRequest request) {
    TemplateEntity entity =
        templateRepository
            .findByIdAndTenantId(templateId, requireTenantId())
            .orElseThrow(() -> new TemplateNotFoundException(templateId));
    if (request.getDescription() != null) {
      entity.setDescription(request.getDescription());
    }
    if (request.getMetadata() != null) {
      entity.setMetadata(request.getMetadata());
    }
    if (request.getDefaultAttachments() != null) {
      entity.setDefaultAttachments(convertAttachments(request.getDefaultAttachments()));
    }
    return templateMapper.toDto(entity);
  }

  @Override
  @CacheEvict(value = {"templates", "activeTemplateVersions"}, key = "#templateId")
  public TemplateDto archiveTemplate(Long templateId) {
    TemplateEntity entity =
        templateRepository
            .findByIdAndTenantId(templateId, requireTenantId())
            .orElseThrow(() -> new TemplateNotFoundException(templateId));
    entity.setArchived(true);
    return templateMapper.toDto(entity);
  }

  @Override
  public TemplateDto cloneTemplate(Long templateId, TemplateCloneRequest request) {
    TemplateEntity source =
        templateRepository
            .findByIdAndTenantId(templateId, requireTenantId())
            .orElseThrow(() -> new TemplateNotFoundException(templateId));
    TemplateEntity clone = new TemplateEntity();
    clone.setName(request.getName());
    clone.setLocale(request.getLocale());
    clone.setDescription(source.getDescription());
    clone.setMetadata(source.getMetadata());
    clone.setDefaultAttachments(copyAttachments(source.getDefaultAttachments()));
    clone.setArchived(false);
    TemplateEntity saved = templateRepository.save(clone);
    if (request.isIncludeVersions()) {
      source.getVersions().forEach(version -> {
        TemplateVersionEntity copy = copyVersion(version);
        copy.setTemplate(saved);
        versionRepository.save(copy);
      });
    }
    return templateMapper.toDto(saved);
  }

  @Override
  @CacheEvict(value = "templates", key = "#templateId")
  public TemplateVersionDto createVersion(Long templateId, TemplateVersionCreateRequest request) {
    TemplateEntity template =
        templateRepository
            .findByIdAndTenantId(templateId, requireTenantId())
            .orElseThrow(() -> new TemplateNotFoundException(templateId));
    TemplateVersionEntity entity = new TemplateVersionEntity();
    entity.setTemplate(template);
    entity.setSubject(request.getSubject());
    entity.setHtmlBody(request.getHtmlBody());
    entity.setTextBody(request.getTextBody());
    entity.setMetadata(request.getMetadata());
    entity.setAllowedVariables(new LinkedHashSet<>(request.getAllowedVariables()));
    entity.setAttachments(convertAttachments(request.getAttachments()));
    int versionNumber =
        versionRepository
            .findFirstByTemplateIdOrderByVersionNumberDesc(templateId)
            .map(v -> v.getVersionNumber() + 1)
            .orElse(1);
    entity.setVersionNumber(versionNumber);
    TemplateVersionEntity saved = versionRepository.save(entity);
    return templateMapper.toDto(saved);
  }

  @Override
  @CacheEvict(
      value = {"templates", "templateVersions", "activeTemplateVersions"},
      allEntries = true)
  public TemplateVersionDto publishVersion(Long templateId, Long versionId) {
    String tenantId = requireTenantId();
    TemplateEntity template =
        templateRepository
            .findByIdAndTenantId(templateId, tenantId)
            .orElseThrow(() -> new TemplateNotFoundException(templateId));
    TemplateVersionEntity version =
        versionRepository
            .findByIdAndTemplateIdAndTemplate_TenantId(versionId, templateId, tenantId)
            .orElseThrow(() -> new TemplateVersionNotFoundException(templateId, versionId));
    version.setStatus(TemplateVersionStatus.PUBLISHED);
    version.setPublishedAt(Instant.now());
    if (version.getSendGridTemplateId() == null) {
      version.setSendGridTemplateId(template.getName());
    }
    sendGridTemplateClient.syncTemplate(template, version);
    sendGridTemplateClient.publishVersion(template, version);
    return templateMapper.toDto(version);
  }

  @Override
  @Cacheable(
      value = "templateVersions",
      key = "T(com.ejada.common.context.ContextManager$Tenant).get() + ':' + #templateId + ':' + #versionId")
  public TemplateVersionDto getVersion(Long templateId, Long versionId) {
    TemplateVersionEntity version =
        versionRepository
            .findByIdAndTemplateIdAndTemplate_TenantId(versionId, templateId, requireTenantId())
            .orElseThrow(() -> new TemplateVersionNotFoundException(templateId, versionId));
    return templateMapper.toDto(version);
  }

  @Override
  public TemplatePreviewResponse preview(Long templateId, Long versionId, TemplatePreviewRequest request) {
    TemplateVersionEntity version =
        versionRepository
            .findByIdAndTemplateIdAndTemplate_TenantId(versionId, templateId, requireTenantId())
            .orElseThrow(() -> new TemplateVersionNotFoundException(templateId, versionId));
    return templateRenderer.render(version, request.getDynamicData());
  }

  @Override
  public TemplateValidationResponse validate(Long templateId, Long versionId, TemplateValidationRequest request) {
    TemplateVersionEntity version =
        versionRepository
            .findByIdAndTemplateIdAndTemplate_TenantId(versionId, templateId, requireTenantId())
            .orElseThrow(() -> new TemplateVersionNotFoundException(templateId, versionId));
    return templateValidator.validate(version, request.getDynamicData());
  }

  @Override
  public Page<TemplateDto> listTemplates(Pageable pageable) {
    Pageable sanitizedPageable = sanitizePageable(pageable);
    return templateRepository
        .findByTenantId(requireTenantId(), sanitizedPageable)
        .map(templateMapper::toDto);
  }

  private Pageable sanitizePageable(Pageable pageable) {
    if (pageable == null || pageable.isUnpaged() || pageable.getSort().isUnsorted()) {
      return pageable;
    }

    List<Sort.Order> allowedOrders = new ArrayList<>();
    pageable
        .getSort()
        .forEach(
            order -> {
              if (ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                allowedOrders.add(order);
              }
            });

    if (allowedOrders.isEmpty()) {
      return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(allowedOrders));
  }

  private TemplateVersionEntity copyVersion(TemplateVersionEntity version) {
    TemplateVersionEntity copy = new TemplateVersionEntity();
    copy.setSubject(version.getSubject());
    copy.setHtmlBody(version.getHtmlBody());
    copy.setTextBody(version.getTextBody());
    copy.setMetadata(version.getMetadata());
    copy.setAllowedVariables(
        version.getAllowedVariables() == null
            ? new LinkedHashSet<>()
            : new LinkedHashSet<>(version.getAllowedVariables()));
    copy.setAttachments(copyAttachments(version.getAttachments()));
    copy.setVersionNumber(version.getVersionNumber());
    copy.setStatus(TemplateVersionStatus.DRAFT);
    copy.setPublishedAt(null);
    return copy;
  }

  private Set<AttachmentMetadata> convertAttachments(List<AttachmentMetadataDto> attachments) {
    if (attachments == null || attachments.isEmpty()) {
      return new LinkedHashSet<>();
    }
    Set<AttachmentMetadata> mapped = new LinkedHashSet<>();
    attachments.stream().filter(dto -> dto != null).forEach(dto -> mapped.add(copyAttachment(dto)));
    return mapped;
  }

  private Set<AttachmentMetadata> copyAttachments(Set<AttachmentMetadata> attachments) {
    if (attachments == null || attachments.isEmpty()) {
      return new LinkedHashSet<>();
    }
    Set<AttachmentMetadata> copies = new LinkedHashSet<>();
    attachments.stream().filter(item -> item != null).forEach(item -> copies.add(copyAttachment(item)));
    return copies;
  }

  private AttachmentMetadata copyAttachment(AttachmentMetadataDto dto) {
    return AttachmentMetadata.builder()
        .fileName(dto.getFileName())
        .contentType(dto.getContentType())
        .storageUrl(dto.getStorageUrl())
        .sizeInBytes(dto.getSizeInBytes())
        .inline(dto.getInline())
        .build();
  }

  private AttachmentMetadata copyAttachment(AttachmentMetadata attachment) {
    return AttachmentMetadata.builder()
        .fileName(attachment.getFileName())
        .contentType(attachment.getContentType())
        .storageUrl(attachment.getStorageUrl())
        .sizeInBytes(attachment.getSizeInBytes())
        .inline(attachment.isInline())
        .build();
  }

  private String requireTenantId() {
    String tenantId = ContextManager.Tenant.get();
    if (tenantId == null) {
      throw new ValidationException("Tenant context is missing", "tenantId is required");
    }
    return tenantId;
  }
}
