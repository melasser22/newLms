package com.ejada.template.gateway.service;

import com.ejada.template.dto.TemplateDto;
import com.ejada.template.gateway.dto.TemplateSummaryView;
import com.ejada.template.service.TemplateService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class TemplateGatewayFacade {

  private final TemplateService templateService;

  public TemplateGatewayFacade(TemplateService templateService) {
    this.templateService = templateService;
  }

  public List<TemplateSummaryView> summaries() {
    return templateService.listTemplates(PageRequest.of(0, 100)).stream()
        .map(this::toSummary)
        .collect(Collectors.toList());
  }

  private TemplateSummaryView toSummary(TemplateDto dto) {
    return new TemplateSummaryView(dto.getId(), dto.getName(), dto.getLocale(), dto.isArchived(), dto.getUpdatedAt());
  }
}
