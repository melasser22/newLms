package com.ejada.email.template.gateway.controller;

import com.ejada.email.template.gateway.dto.TemplateSummaryView;
import com.ejada.email.template.gateway.dto.TemplateSyncRequest;
import com.ejada.email.template.gateway.service.TemplateGatewayFacade;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api/v1/tenants/{tenantId}/templates")
public class TenantTemplateGatewayController {

  private final TemplateGatewayFacade facade;

  public TenantTemplateGatewayController(TemplateGatewayFacade facade) {
    this.facade = facade;
  }

  @GetMapping
  public List<TemplateSummaryView> summaries(@PathVariable String tenantId) {
    return facade.summaries();
  }

  @PostMapping("/sync")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void sync(@PathVariable String tenantId, @Valid @RequestBody TemplateSyncRequest request) {
    // In a full implementation this would kick off a SendGrid synchronization job scoped to the
    // tenant. For now the endpoint simply acknowledges the request so the orchestrator can proceed.
  }
}
