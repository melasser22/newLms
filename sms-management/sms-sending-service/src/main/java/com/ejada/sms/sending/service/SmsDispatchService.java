package com.ejada.sms.sending.service;

import com.ejada.sms.sending.dto.BulkSmsSendRequest;
import com.ejada.sms.sending.dto.SmsSendRequest;
import com.ejada.sms.sending.dto.SmsSendResponse;

public interface SmsDispatchService {
  SmsSendResponse send(String tenantId, SmsSendRequest request);

  void sendBulk(String tenantId, BulkSmsSendRequest request);
}
