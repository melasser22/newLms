package com.ejada.email.template.dto;

import com.ejada.email.template.domain.enums.EmailEventType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EmailStatsPoint {
  EmailEventType type;
  long total;
}
