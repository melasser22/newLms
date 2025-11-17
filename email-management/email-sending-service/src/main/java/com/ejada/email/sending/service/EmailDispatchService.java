package com.ejada.email.sending.service;

import com.ejada.email.sending.dto.BulkEmailSendRequest;
import com.ejada.email.sending.dto.EmailSendRequest;
import com.ejada.email.sending.dto.EmailSendResponse;

public interface EmailDispatchService {
  EmailSendResponse sendEmail(String tenantId, EmailSendRequest request);

  void sendBulk(String tenantId, BulkEmailSendRequest request);
}
