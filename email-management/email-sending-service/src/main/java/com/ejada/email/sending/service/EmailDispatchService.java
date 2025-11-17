package com.ejada.sending.service;

import com.ejada.sending.dto.BulkEmailSendRequest;
import com.ejada.sending.dto.EmailSendRequest;
import com.ejada.sending.dto.EmailSendResponse;

public interface EmailDispatchService {
  EmailSendResponse sendEmail(String tenantId, EmailSendRequest request);

  void sendBulk(String tenantId, BulkEmailSendRequest request);
}
