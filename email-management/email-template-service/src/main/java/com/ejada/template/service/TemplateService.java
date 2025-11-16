package com.ejada.template.service;

import com.ejada.template.dto.TemplateCloneRequest;
import com.ejada.template.dto.TemplateDto;
import com.ejada.template.dto.TemplatePreviewRequest;
import com.ejada.template.dto.TemplatePreviewResponse;
import com.ejada.template.dto.TemplateValidationRequest;
import com.ejada.template.dto.TemplateValidationResponse;
import com.ejada.template.dto.TemplateVersionCreateRequest;
import com.ejada.template.dto.TemplateVersionDto;
import com.ejada.template.dto.UpdateTemplateRequest;
import com.ejada.template.dto.CreateTemplateRequest;
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
