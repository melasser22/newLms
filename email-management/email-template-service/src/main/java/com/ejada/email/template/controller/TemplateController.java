package com.ejada.email.template.controller;

import com.ejada.common.dto.BaseResponse;
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
import com.ejada.email.template.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

  private final TemplateService templateService;

  public TemplateController(TemplateService templateService) {
    this.templateService = templateService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BaseResponse<TemplateDto> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
    return BaseResponse.success(templateService.createTemplate(request));
  }

  @PutMapping("/{templateId}")
  public BaseResponse<TemplateDto> updateTemplate(
      @PathVariable Long templateId, @Valid @RequestBody UpdateTemplateRequest request) {
    return BaseResponse.success(templateService.updateTemplate(templateId, request));
  }

  @PostMapping("/{templateId}/archive")
  public BaseResponse<TemplateDto> archiveTemplate(@PathVariable Long templateId) {
    return BaseResponse.success(templateService.archiveTemplate(templateId));
  }

  @PostMapping("/{templateId}/clone")
  public BaseResponse<TemplateDto> cloneTemplate(
      @PathVariable Long templateId, @Valid @RequestBody TemplateCloneRequest request) {
    return BaseResponse.success(templateService.cloneTemplate(templateId, request));
  }

  @GetMapping
  public BaseResponse<Page<TemplateDto>> listTemplates(Pageable pageable) {
    return BaseResponse.success(templateService.listTemplates(pageable));
  }

  @PostMapping("/{templateId}/versions")
  @ResponseStatus(HttpStatus.CREATED)
  public BaseResponse<TemplateVersionDto> createVersion(
      @PathVariable Long templateId, @Valid @RequestBody TemplateVersionCreateRequest request) {
    return BaseResponse.success(templateService.createVersion(templateId, request));
  }

  @PostMapping("/{templateId}/versions/{versionId}/publish")
  public BaseResponse<TemplateVersionDto> publishVersion(
      @PathVariable Long templateId, @PathVariable Long versionId) {
    return BaseResponse.success(templateService.publishVersion(templateId, versionId));
  }

  @GetMapping("/{templateId}/versions/{versionId}")
  public BaseResponse<TemplateVersionDto> getVersion(
      @PathVariable Long templateId, @PathVariable Long versionId) {
    return BaseResponse.success(templateService.getVersion(templateId, versionId));
  }

  @PostMapping("/{templateId}/versions/{versionId}/preview")
  public BaseResponse<TemplatePreviewResponse> previewVersion(
      @PathVariable Long templateId,
      @PathVariable Long versionId,
      @Valid @RequestBody TemplatePreviewRequest request) {
    return BaseResponse.success(templateService.preview(templateId, versionId, request));
  }

  @PostMapping("/{templateId}/versions/{versionId}/validate")
  public BaseResponse<TemplateValidationResponse> validate(
      @PathVariable Long templateId,
      @PathVariable Long versionId,
      @Valid @RequestBody TemplateValidationRequest request) {
    return BaseResponse.success(templateService.validate(templateId, versionId, request));
  }
}
