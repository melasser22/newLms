package com.ejada.email.template.service.impl;

import com.ejada.common.context.ContextManager;
import com.ejada.email.template.domain.entity.EmailSendEntity;
import com.ejada.email.template.domain.entity.TemplateEntity;
import com.ejada.email.template.domain.entity.TemplateVersionEntity;
import com.ejada.email.template.domain.enums.EmailSendMode;
import com.ejada.email.template.domain.enums.EmailSendStatus;
import com.ejada.email.template.domain.value.AttachmentMetadata;
import com.ejada.email.template.dto.AttachmentMetadataDto;
import com.ejada.email.template.dto.BulkEmailSendRequest;
import com.ejada.email.template.dto.EmailSendRequest;
import com.ejada.email.template.dto.EmailSendResponse;
import com.ejada.email.template.exception.TemplateNotFoundException;
import com.ejada.email.template.exception.TemplateVersionNotFoundException;
import com.ejada.email.template.exception.TemplateValidationException;
import com.ejada.email.template.messaging.model.EmailSendMessage;
import com.ejada.email.template.messaging.producer.EmailSendProducer;
import com.ejada.email.template.repository.EmailSendRepository;
import com.ejada.email.template.repository.TemplateRepository;
import com.ejada.email.template.repository.TemplateVersionRepository;
import com.ejada.email.template.service.EmailSendService;
import com.ejada.email.template.service.support.TemplateLookupService;
import com.ejada.email.template.service.support.TemplateValidator;
import com.ejada.email.template.service.support.RateLimiterService;
import com.ejada.email.template.service.support.RedisIdempotencyService;
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
  private final TemplateValidator templateValidator;
  private final TemplateLookupService templateLookupService;

  @Override
  public EmailSendResponse sendEmail(EmailSendRequest request) {
    String tenantId = requireTenantId();
    rateLimiterService.validateQuota(tenantId, "email-send");

    if (request.getIdempotencyKey() != null) {
      var existing = idempotencyService.findSendId(tenantId, request.getIdempotencyKey());
      if (existing.isPresent()) {
        EmailSendEntity entity =
            emailSendRepository
                .findByIdAndTenantId(existing.get(), tenantId)
                .orElseThrow(() -> new IllegalStateException("Idempotent send missing"));
        return buildResponse(entity);
      }
    }

    TemplateVersionEntity version = resolveVersion(request, tenantId);
    validateDynamicData(version, request);

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

  private TemplateVersionEntity resolveVersion(EmailSendRequest request, String tenantId) {
    if (request.getTemplateVersionId() != null) {
      return templateVersionRepository
          .findByIdAndTemplateIdAndTemplate_TenantId(
              request.getTemplateVersionId(), request.getTemplateId(), tenantId)
          .orElseThrow(() -> new TemplateVersionNotFoundException(request.getTemplateId(), request.getTemplateVersionId()));
    }
    TemplateEntity template =
        templateRepository
            .findByIdAndTenantId(request.getTemplateId(), tenantId)
            .orElseThrow(() -> new TemplateNotFoundException(request.getTemplateId()));
    return templateLookupService.getActivePublishedVersion(template.getId());
  }

  private void validateDynamicData(TemplateVersionEntity version, EmailSendRequest request) {
    var validation = templateValidator.validate(version, request.getDynamicData());
    if (!validation.isValid()) {
      throw new TemplateValidationException(validation);
    }
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

  private String requireTenantId() {
    return Objects.requireNonNull(ContextManager.Tenant.get(), "tenantId is required");
  }
}
