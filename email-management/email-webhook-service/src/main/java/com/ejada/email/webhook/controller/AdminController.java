package com.ejada.email.webhook.controller;

import com.ejada.email.webhook.model.EmailEvent;
import com.ejada.email.webhook.model.EmailEventType;
import com.ejada.email.webhook.model.EmailLogStatus;
import com.ejada.email.webhook.repository.EmailEventRepository;
import com.ejada.email.webhook.repository.EmailLogRepository;
import com.ejada.email.webhook.service.SendgridEventProcessor;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

  private final EmailEventRepository emailEventRepository;
  private final EmailLogRepository emailLogRepository;
  private final SendgridEventProcessor processor;

  public AdminController(
      EmailEventRepository emailEventRepository,
      EmailLogRepository emailLogRepository,
      SendgridEventProcessor processor) {
    this.emailEventRepository = emailEventRepository;
    this.emailLogRepository = emailLogRepository;
    this.processor = processor;
  }

  @GetMapping("/events")
  public List<EmailEvent> listEvents(@RequestParam(name = "type", required = false) String type) {
    if (type == null) {
      return emailEventRepository.findAll();
    }
    return emailEventRepository.findByType(EmailEventType.valueOf(type));
  }

  @GetMapping("/email-logs")
  public Map<String, EmailLogStatus> emailLogs() {
    return emailLogRepository.findAll();
  }

  @GetMapping("/analytics")
  public Map<String, Long> analytics() {
    return processor.analytics();
  }
}
