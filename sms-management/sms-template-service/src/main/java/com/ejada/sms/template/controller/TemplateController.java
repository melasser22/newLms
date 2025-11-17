package com.ejada.sms.template.controller;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/templates")
public class TemplateController {

  private final Map<String, Map<String, TemplateDto>> templates = new ConcurrentHashMap<>();

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TemplateDto create(
      @PathVariable String tenantId, @Valid @RequestBody TemplateDto request) {
    templates
        .computeIfAbsent(tenantId, t -> new ConcurrentHashMap<>())
        .put(request.code() + '|' + request.locale(), request);
    return request;
  }

  @GetMapping("/{code}/{locale}")
  public TemplateDto get(
      @PathVariable String tenantId, @PathVariable String code, @PathVariable String locale) {
    return templates
        .getOrDefault(tenantId, Map.of())
        .get(code + '|' + locale);
  }

  @GetMapping
  public List<TemplateDto> list(@PathVariable String tenantId) {
    return new ArrayList<>(templates.getOrDefault(tenantId, Map.of()).values());
  }
}
