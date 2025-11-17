package com.ejada.sms.sending.controller;

import com.ejada.sms.sending.dto.BulkSmsSendRequest;
import com.ejada.sms.sending.dto.SmsSendRequest;
import com.ejada.sms.sending.dto.SmsSendResponse;
import com.ejada.sms.sending.service.SmsDispatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/sms")
public class SmsSendController {

  private final SmsDispatchService dispatchService;

  public SmsSendController(SmsDispatchService dispatchService) {
    this.dispatchService = dispatchService;
  }

  @PostMapping("/send")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public SmsSendResponse send(@PathVariable String tenantId, @Valid @RequestBody SmsSendRequest request) {
    return dispatchService.send(tenantId, request);
  }

  @PostMapping("/send/bulk")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void sendBulk(@PathVariable String tenantId, @Valid @RequestBody BulkSmsSendRequest request) {
    dispatchService.sendBulk(tenantId, request);
  }
}
