package com.ejada.email.template.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.email.template.dto.BulkEmailSendRequest;
import com.ejada.email.template.dto.EmailSendRequest;
import com.ejada.email.template.dto.EmailSendResponse;
import com.ejada.email.template.service.EmailSendService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/emails")
public class EmailController {

  private final EmailSendService emailSendService;

  public EmailController(EmailSendService emailSendService) {
    this.emailSendService = emailSendService;
  }

  @PostMapping("/send")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public BaseResponse<EmailSendResponse> sendEmail(@Valid @RequestBody EmailSendRequest request) {
    return BaseResponse.success(emailSendService.sendEmail(request));
  }

  @PostMapping("/send/bulk")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public BaseResponse<java.util.List<EmailSendResponse>> sendBulk(
      @Valid @RequestBody BulkEmailSendRequest request) {
    return BaseResponse.success(emailSendService.sendBulkEmails(request));
  }
}
