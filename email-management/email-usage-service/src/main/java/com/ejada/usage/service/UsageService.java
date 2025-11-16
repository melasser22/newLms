package com.ejada.usage.service;

import com.ejada.usage.dto.UsageReportDto;
import java.time.LocalDate;

public interface UsageService {
  UsageReportDto report(String tenantId, LocalDate from, LocalDate to);
}
