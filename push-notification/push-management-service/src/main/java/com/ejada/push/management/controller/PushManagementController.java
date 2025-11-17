package com.ejada.push.management.controller;

import com.ejada.push.management.dto.DeviceRegistrationRequest;
import com.ejada.push.management.dto.DeviceRegistrationResponse;
import com.ejada.push.management.dto.SendRequest;
import com.ejada.push.management.dto.SendResponse;
import com.ejada.push.management.dto.TemplateResponse;
import com.ejada.push.management.dto.TemplateUpsertRequest;
import com.ejada.push.management.dto.UsageSummary;
import com.ejada.push.management.service.DeviceGatewayService;
import com.ejada.push.management.service.SendingGatewayService;
import com.ejada.push.management.service.TemplateGatewayService;
import com.ejada.push.management.service.UsageGatewayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}")
public class PushManagementController {

  private final TemplateGatewayService templateGatewayService;
  private final SendingGatewayService sendingGatewayService;
  private final DeviceGatewayService deviceGatewayService;
  private final UsageGatewayService usageGatewayService;

  public PushManagementController(
      TemplateGatewayService templateGatewayService,
      SendingGatewayService sendingGatewayService,
      DeviceGatewayService deviceGatewayService,
      UsageGatewayService usageGatewayService) {
    this.templateGatewayService = templateGatewayService;
    this.sendingGatewayService = sendingGatewayService;
    this.deviceGatewayService = deviceGatewayService;
    this.usageGatewayService = usageGatewayService;
  }

  @PostMapping("/templates")
  @ResponseStatus(HttpStatus.CREATED)
  public TemplateResponse upsertTemplate(
      @PathVariable String tenantId, @Valid @RequestBody TemplateUpsertRequest request) {
    return templateGatewayService.upsertTemplate(tenantId, request);
  }

  @GetMapping("/templates/{key}/active")
  public TemplateResponse getActiveTemplate(
      @PathVariable String tenantId, @PathVariable String key) {
    return templateGatewayService.getActive(tenantId, key);
  }

  @PostMapping("/send")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public SendResponse send(
      @PathVariable String tenantId, @Valid @RequestBody SendRequest request) {
    return sendingGatewayService.send(tenantId, request);
  }

  @PostMapping("/devices")
  @ResponseStatus(HttpStatus.CREATED)
  public DeviceRegistrationResponse registerDevice(
      @PathVariable String tenantId, @Valid @RequestBody DeviceRegistrationRequest request) {
    return deviceGatewayService.register(tenantId, request);
  }

  @GetMapping("/usage/daily")
  public UsageSummary getUsage(@PathVariable String tenantId) {
    return usageGatewayService.getDailySummary(tenantId);
  }
}
