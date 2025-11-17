package com.ejada.template.dto;

import com.ejada.template.domain.enums.EmailSendStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EmailSendResponse {
  Long sendId;
  EmailSendStatus status;
  String idempotencyKey;
}
