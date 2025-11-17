package com.ejada.email.template.messaging.model;

import com.ejada.email.template.domain.enums.EmailSendMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EmailSendMessage {
  Long sendId;
  Long templateId;
  Long templateVersionId;
  List<String> recipients;
  List<String> cc;
  List<String> bcc;
  Map<String, Object> dynamicData;
  EmailSendMode mode;
  Instant requestedAt;
  Map<String, Object> customArgs;
}
