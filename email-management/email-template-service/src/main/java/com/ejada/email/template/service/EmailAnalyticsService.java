package com.ejada.email.template.service;

import com.ejada.email.template.dto.EmailStatsResponse;
import java.time.Instant;

public interface EmailAnalyticsService {
  EmailStatsResponse getEmailStats(Instant from, Instant to);
}
