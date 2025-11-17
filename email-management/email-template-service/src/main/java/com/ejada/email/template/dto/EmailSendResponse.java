package com.ejada.email.template.dto;

import com.ejada.email.template.domain.enums.EmailSendStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EmailSendResponse {
  Long sendId;
  EmailSendStatus status;
  String idempotencyKey;
}
