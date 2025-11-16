package com.ejada.template.service.impl;

import com.ejada.template.domain.entity.TemplateEntity;
import com.ejada.template.domain.entity.TemplateVersionEntity;
import com.ejada.template.domain.enums.TemplateVersionStatus;
import com.ejada.template.domain.value.AttachmentMetadata;
import com.ejada.template.dto.AttachmentMetadataDto;
import com.ejada.template.dto.CreateTemplateRequest;
import com.ejada.template.dto.TemplateCloneRequest;
import com.ejada.template.dto.TemplateDto;
import com.ejada.template.dto.TemplatePreviewRequest;
import com.ejada.template.dto.TemplatePreviewResponse;
import com.ejada.template.dto.TemplateValidationRequest;
import com.ejada.template.dto.TemplateValidationResponse;
import com.ejada.template.dto.TemplateVersionCreateRequest;
import com.ejada.template.dto.TemplateVersionDto;
import com.ejada.template.dto.UpdateTemplateRequest;
import com.ejada.template.exception.TemplateNotFoundException;
import com.ejada.template.exception.TemplateVersionNotFoundException;
import com.ejada.template.mapper.TemplateMapper;
import com.ejada.template.repository.TemplateRepository;
import com.ejada.template.repository.TemplateVersionRepository;
import com.ejada.template.service.TemplateService;
import com.ejada.template.service.support.SendGridTemplateClient;
import com.ejada.template.service.support.TemplateRenderer;
import com.ejada.template.service.support.TemplateValidator;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateServiceImpl implements TemplateService {

  private final TemplateRepository templateRepository;
  private final TemplateVersionRepository versionRepository;
  private final TemplateMapper templateMapper;
  private final TemplateRenderer templateRenderer;
  private final TemplateValidator templateValidator;
  private final SendGridTemplateClient sendGridTemplateClient;

  @Override
  public TemplateDto createTemplate(CreateTemplateRequest request) {
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
  @CacheEvict(value = "templates", key = "#templateId")
  public TemplateDto updateTemplate(Long templateId, UpdateTemplateRequest request) {
    TemplateEntity entity = templateRepository.findById(templateId).orElseThrow(() -> new TemplateNotFoundException(templateId));
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
  @CacheEvict(value = "templates", key = "#templateId")
  public TemplateDto archiveTemplate(Long templateId) {
    TemplateEntity entity = templateRepository.findById(templateId).orElseThrow(() -> new TemplateNotFoundException(templateId));
    entity.setArchived(true);
    return templateMapper.toDto(entity);
  }

  @Override
  public TemplateDto cloneTemplate(Long templateId, TemplateCloneRequest request) {
    TemplateEntity source = templateRepository.findById(templateId).orElseThrow(() -> new TemplateNotFoundException(templateId));
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
    TemplateEntity template = templateRepository.findById(templateId).orElseThrow(() -> new TemplateNotFoundException(templateId));
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
  @CacheEvict(value = {"templates", "templateVersions"}, allEntries = true)
  public TemplateVersionDto publishVersion(Long templateId, Long versionId) {
    TemplateEntity template = templateRepository.findById(templateId).orElseThrow(() -> new TemplateNotFoundException(templateId));
    TemplateVersionEntity version = versionRepository.findById(versionId).orElseThrow(() -> new TemplateVersionNotFoundException(templateId, versionId));
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
    TemplateVersionEntity version = versionRepository.findById(versionId).orElseThrow(() -> new TemplateVersionNotFoundException(templateId, versionId));
    return templateMapper.toDto(version);
  }

  @Override
  public TemplatePreviewResponse preview(Long templateId, Long versionId, TemplatePreviewRequest request) {
    TemplateVersionEntity version = versionRepository.findById(versionId).orElseThrow(() -> new TemplateVersionNotFoundException(templateId, versionId));
    return templateRenderer.render(version, request.getDynamicData());
  }

  @Override
  public TemplateValidationResponse validate(Long templateId, Long versionId, TemplateValidationRequest request) {
    TemplateVersionEntity version = versionRepository.findById(versionId).orElseThrow(() -> new TemplateVersionNotFoundException(templateId, versionId));
    return templateValidator.validate(version, request.getDynamicData());
  }

  @Override
  public Page<TemplateDto> listTemplates(Pageable pageable) {
    return templateRepository.findAll(pageable).map(templateMapper::toDto);
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
}
