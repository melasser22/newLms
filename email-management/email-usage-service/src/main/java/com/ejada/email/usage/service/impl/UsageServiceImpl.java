package com.ejada.email.usage.service.impl;

import com.ejada.email.usage.domain.UsageAggregate;
import com.ejada.email.usage.dto.UsageMetricDto;
import com.ejada.email.usage.dto.UsageReportDto;
import com.ejada.email.usage.repository.UsageRepository;
import com.ejada.email.usage.service.UsageService;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class UsageServiceImpl implements UsageService {

  private final UsageRepository repository;

  public UsageServiceImpl(UsageRepository repository) {
    this.repository = repository;
  }

  @Override
  public UsageReportDto report(String tenantId, LocalDate from, LocalDate to) {
    List<UsageAggregate> aggregates = repository.loadUsage(tenantId, from, to);
    List<UsageMetricDto> metrics =
        aggregates.stream()
            .map(a -> new UsageMetricDto(a.date(), a.delivered(), a.bounced(), a.complaints()))
            .collect(Collectors.toList());
    return new UsageReportDto(tenantId, metrics);
  }
}
