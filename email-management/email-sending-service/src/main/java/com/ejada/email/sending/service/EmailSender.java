package com.ejada.sending.service;

import com.ejada.sending.messaging.EmailEnvelope;

public interface EmailSender {
  void send(EmailEnvelope envelope);
}
