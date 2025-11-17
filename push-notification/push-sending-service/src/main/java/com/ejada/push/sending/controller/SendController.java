package com.ejada.push.sending.controller;

import com.ejada.push.sending.model.SendLog;
import com.ejada.push.sending.service.SendRequest;
import com.ejada.push.sending.service.SendResponse;
import com.ejada.push.sending.service.SendService;
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
@RequestMapping("/api/v1/tenants/{tenantId}")
public class SendController {

  private final SendService sendService;

  public SendController(SendService sendService) {
    this.sendService = sendService;
  }

  @PostMapping("/send")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public SendResponse send(
      @PathVariable String tenantId, @Valid @RequestBody SendRequest request) {
    return sendService.enqueue(tenantId, request);
  }

  @GetMapping("/send/logs")
  public List<SendLog> logs(@PathVariable String tenantId) {
    return sendService.list(tenantId);
  }
}
