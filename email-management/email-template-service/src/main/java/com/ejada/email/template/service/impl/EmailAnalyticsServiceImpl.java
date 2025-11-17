package com.ejada.email.template.service.impl;

import com.ejada.email.template.dto.EmailStatsPoint;
import com.ejada.email.template.dto.EmailStatsResponse;
import com.ejada.email.template.repository.EmailEventRepository;
import com.ejada.email.template.service.EmailAnalyticsService;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailAnalyticsServiceImpl implements EmailAnalyticsService {

  private final EmailEventRepository emailEventRepository;

  @Override
  public EmailStatsResponse getEmailStats(Instant from, Instant to) {
    List<EmailStatsPoint> points = emailEventRepository.aggregateBetween(from, to).stream()
        .map(aggregation -> EmailStatsPoint.builder()
            .type(aggregation.getType())
            .total(aggregation.getTotal())
            .build())
        .collect(Collectors.toList());
    return EmailStatsResponse.builder().from(from).to(to).points(points).build();
  }
}
