package com.ejada.template.service;

import com.ejada.template.dto.EmailStatsResponse;
import java.time.Instant;

public interface EmailAnalyticsService {
  EmailStatsResponse getEmailStats(Instant from, Instant to);
}
