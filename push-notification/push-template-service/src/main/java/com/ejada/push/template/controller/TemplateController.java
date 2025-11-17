package com.ejada.push.template.controller;

import com.ejada.push.template.model.TemplateVersion;
import com.ejada.push.template.service.TemplatePreview;
import com.ejada.push.template.service.TemplatePreviewRequest;
import com.ejada.push.template.service.TemplateRequest;
import com.ejada.push.template.service.TemplateResponse;
import com.ejada.push.template.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/templates")
public class TemplateController {

  private final TemplateService templateService;

  public TemplateController(TemplateService templateService) {
    this.templateService = templateService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TemplateResponse upsert(
      @PathVariable String tenantId, @Valid @RequestBody TemplateRequest request) {
    return templateService.upsertTemplate(tenantId, request);
  }

  @GetMapping("/{key}/active")
  public TemplateResponse getActive(
      @PathVariable String tenantId,
      @PathVariable String key,
      @RequestParam(value = "locale", required = false) String locale) {
    TemplateVersion version = templateService.getActiveTemplate(tenantId, key, locale);
    if (version == null) {
      return null;
    }
    return new TemplateResponse(key, version.getLocale(), version.getVersion(), version.isActive());
  }

  @PostMapping("/{key}/preview")
  public TemplatePreview preview(
      @PathVariable String tenantId,
      @PathVariable String key,
      @RequestBody TemplatePreviewRequest request) {
    return templateService.previewTemplate(tenantId, key, request);
  }
}
