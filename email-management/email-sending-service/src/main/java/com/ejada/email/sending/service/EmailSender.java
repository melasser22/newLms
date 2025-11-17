package com.ejada.email.sending.service;

import com.ejada.email.sending.messaging.EmailEnvelope;

public interface EmailSender {
  void send(EmailEnvelope envelope);
}
