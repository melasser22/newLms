package com.ejada.template.service.impl;

import com.ejada.common.context.ContextManager;
import com.ejada.template.domain.entity.EmailSendEntity;
import com.ejada.template.domain.entity.TemplateEntity;
import com.ejada.template.domain.entity.TemplateVersionEntity;
import com.ejada.template.domain.enums.EmailSendMode;
import com.ejada.template.domain.enums.EmailSendStatus;
import com.ejada.template.domain.enums.TemplateVersionStatus;
import com.ejada.template.domain.value.AttachmentMetadata;
import com.ejada.template.dto.AttachmentMetadataDto;
import com.ejada.template.dto.BulkEmailSendRequest;
import com.ejada.template.dto.EmailSendRequest;
import com.ejada.template.dto.EmailSendResponse;
import com.ejada.template.exception.TemplateNotFoundException;
import com.ejada.template.exception.TemplateVersionNotFoundException;
import com.ejada.template.messaging.model.EmailSendMessage;
import com.ejada.template.messaging.producer.EmailSendProducer;
import com.ejada.template.repository.EmailSendRepository;
import com.ejada.template.repository.TemplateRepository;
import com.ejada.template.repository.TemplateVersionRepository;
import com.ejada.template.service.EmailSendService;
import com.ejada.template.service.support.RateLimiterService;
import com.ejada.template.service.support.RedisIdempotencyService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailSendServiceImpl implements EmailSendService {

  private final EmailSendRepository emailSendRepository;
  private final TemplateRepository templateRepository;
  private final TemplateVersionRepository templateVersionRepository;
  private final EmailSendProducer emailSendProducer;
  private final RedisIdempotencyService idempotencyService;
  private final RateLimiterService rateLimiterService;

  @Override
  public EmailSendResponse sendEmail(EmailSendRequest request) {
    String tenantId = ContextManager.Tenant.get();
    rateLimiterService.validateQuota(tenantId, "email-send");

    if (request.getIdempotencyKey() != null) {
      var existing = idempotencyService.findSendId(tenantId, request.getIdempotencyKey());
      if (existing.isPresent()) {
        EmailSendEntity entity =
            emailSendRepository
                .findById(existing.get())
                .orElseThrow(() -> new IllegalStateException("Idempotent send missing"));
        return buildResponse(entity);
      }
    }

    TemplateVersionEntity version = resolveVersion(request);
    EmailSendEntity entity = new EmailSendEntity();
    entity.setTemplateVersion(version);
    entity.setRecipients(request.getRecipients());
    entity.setCc(request.getCc());
    entity.setBcc(request.getBcc());
    entity.setDynamicData(request.getDynamicData());
    entity.setAttachments(mergeAttachments(version, request.getAttachments()));
    EmailSendMode mode = request.getMode() == null ? EmailSendMode.PRODUCTION : request.getMode();
    entity.setMode(mode);
    entity.setRequestedAt(Instant.now());
    entity.setIdempotencyKey(request.getIdempotencyKey());
    entity.setStatus(resolveStatus(mode));

    EmailSendEntity saved = emailSendRepository.save(entity);
    if (request.getIdempotencyKey() != null) {
      idempotencyService.storeSendId(tenantId, request.getIdempotencyKey(), saved.getId());
    }

    EmailSendMessage message = EmailSendMessage.builder()
        .sendId(saved.getId())
        .templateId(request.getTemplateId())
        .templateVersionId(saved.getTemplateVersion().getId())
        .recipients(saved.getRecipients())
        .cc(saved.getCc())
        .bcc(saved.getBcc())
        .dynamicData(saved.getDynamicData())
        .mode(saved.getMode())
        .requestedAt(saved.getRequestedAt())
        .customArgs(request.getCustomArgs())
        .build();
    emailSendProducer.publish(message);
    return buildResponse(saved);
  }

  @Override
  public List<EmailSendResponse> sendBulkEmails(BulkEmailSendRequest request) {
    return request.getSends().stream().map(this::sendEmail).collect(Collectors.toList());
  }

  private TemplateVersionEntity resolveVersion(EmailSendRequest request) {
    if (request.getTemplateVersionId() != null) {
      return templateVersionRepository
          .findById(request.getTemplateVersionId())
          .filter(version -> version.getTemplate() != null && version.getTemplate().getId().equals(request.getTemplateId()))
          .orElseThrow(() -> new TemplateVersionNotFoundException(request.getTemplateId(), request.getTemplateVersionId()));
    }
    TemplateEntity template = templateRepository.findById(request.getTemplateId()).orElseThrow(() -> new TemplateNotFoundException(request.getTemplateId()));
    return templateVersionRepository
        .findFirstByTemplateIdAndStatusOrderByVersionNumberDesc(template.getId(), TemplateVersionStatus.PUBLISHED)
        .orElseThrow(() -> new TemplateVersionNotFoundException(request.getTemplateId(), null));
  }

  private Set<AttachmentMetadata> mergeAttachments(
      TemplateVersionEntity version, List<AttachmentMetadataDto> overrides) {
    Set<AttachmentMetadata> merged = new LinkedHashSet<>();
    if (version.getTemplate() != null) {
      merged.addAll(copyAttachments(version.getTemplate().getDefaultAttachments()));
    }
    merged.addAll(copyAttachments(version.getAttachments()));
    if (overrides != null) {
      overrides.stream().filter(Objects::nonNull).forEach(dto -> merged.add(copyAttachment(dto)));
    }
    return merged;
  }

  private Set<AttachmentMetadata> copyAttachments(Set<AttachmentMetadata> attachments) {
    if (attachments == null) {
      return Set.of();
    }
    return attachments.stream().map(this::copyAttachment).collect(Collectors.toCollection(LinkedHashSet::new));
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

  private AttachmentMetadata copyAttachment(AttachmentMetadataDto dto) {
    return AttachmentMetadata.builder()
        .fileName(dto.getFileName())
        .contentType(dto.getContentType())
        .storageUrl(dto.getStorageUrl())
        .sizeInBytes(dto.getSizeInBytes())
        .inline(Boolean.TRUE.equals(dto.getInline()))
        .build();
  }

  private EmailSendStatus resolveStatus(EmailSendMode mode) {
    return mode == EmailSendMode.PRODUCTION ? EmailSendStatus.QUEUED : EmailSendStatus.TESTED;
  }

  private EmailSendResponse buildResponse(EmailSendEntity entity) {
    return EmailSendResponse.builder()
        .sendId(entity.getId())
        .status(entity.getStatus())
        .idempotencyKey(entity.getIdempotencyKey())
        .build();
  }
}
