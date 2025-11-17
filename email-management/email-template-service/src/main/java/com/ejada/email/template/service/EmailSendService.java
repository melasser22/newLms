package com.ejada.email.template.service;

import com.ejada.email.template.dto.BulkEmailSendRequest;
import com.ejada.email.template.dto.EmailSendRequest;
import com.ejada.email.template.dto.EmailSendResponse;
import java.util.List;

public interface EmailSendService {
  EmailSendResponse sendEmail(EmailSendRequest request);

  List<EmailSendResponse> sendBulkEmails(BulkEmailSendRequest request);
}
