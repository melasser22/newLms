package com.ejada.template.dto;

import com.ejada.template.domain.enums.EmailEventType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EmailStatsPoint {
  EmailEventType type;
  long total;
}
