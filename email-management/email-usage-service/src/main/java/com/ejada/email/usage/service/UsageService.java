package com.ejada.email.usage.service;

import com.ejada.email.usage.dto.UsageReportDto;
import java.time.LocalDate;

public interface UsageService {
  UsageReportDto report(String tenantId, LocalDate from, LocalDate to);
}
