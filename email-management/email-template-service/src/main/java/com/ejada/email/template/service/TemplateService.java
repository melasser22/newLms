package com.ejada.email.template.service;

import com.ejada.email.template.dto.TemplateCloneRequest;
import com.ejada.email.template.dto.TemplateDto;
import com.ejada.email.template.dto.TemplatePreviewRequest;
import com.ejada.email.template.dto.TemplatePreviewResponse;
import com.ejada.email.template.dto.TemplateValidationRequest;
import com.ejada.email.template.dto.TemplateValidationResponse;
import com.ejada.email.template.dto.TemplateVersionCreateRequest;
import com.ejada.email.template.dto.TemplateVersionDto;
import com.ejada.email.template.dto.UpdateTemplateRequest;
import com.ejada.email.template.dto.CreateTemplateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TemplateService {

  TemplateDto createTemplate(CreateTemplateRequest request);

  TemplateDto updateTemplate(Long templateId, UpdateTemplateRequest request);

  TemplateDto archiveTemplate(Long templateId);

  TemplateDto cloneTemplate(Long templateId, TemplateCloneRequest request);

  TemplateVersionDto createVersion(Long templateId, TemplateVersionCreateRequest request);

  TemplateVersionDto publishVersion(Long templateId, Long versionId);

  TemplateVersionDto getVersion(Long templateId, Long versionId);

  TemplatePreviewResponse preview(Long templateId, Long versionId, TemplatePreviewRequest request);

  TemplateValidationResponse validate(Long templateId, Long versionId, TemplateValidationRequest request);

  Page<TemplateDto> listTemplates(Pageable pageable);
}
