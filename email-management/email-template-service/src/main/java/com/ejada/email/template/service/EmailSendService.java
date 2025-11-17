package com.ejada.template.service;

import com.ejada.template.dto.BulkEmailSendRequest;
import com.ejada.template.dto.EmailSendRequest;
import com.ejada.template.dto.EmailSendResponse;
import java.util.List;

public interface EmailSendService {
  EmailSendResponse sendEmail(EmailSendRequest request);

  List<EmailSendResponse> sendBulkEmails(BulkEmailSendRequest request);
}
