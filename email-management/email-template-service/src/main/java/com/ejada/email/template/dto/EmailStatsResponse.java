package com.ejada.template.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EmailStatsResponse {
  Instant from;
  Instant to;
  List<EmailStatsPoint> points;
}
