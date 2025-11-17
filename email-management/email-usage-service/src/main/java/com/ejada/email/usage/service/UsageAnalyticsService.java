package com.ejada.email.usage.service;

import com.ejada.email.usage.domain.AnomalyAlert;
import com.ejada.email.usage.domain.DailyUsageAggregate;
import com.ejada.email.usage.domain.QuotaStatus;
import com.ejada.email.usage.domain.TenantQuota;
import com.ejada.email.usage.domain.UsageReportRow;
import com.ejada.email.usage.domain.UsageSummary;
import com.ejada.email.usage.domain.UsageTrendPoint;
import com.ejada.email.usage.repository.DailyUsageAggregateRepository;
import com.ejada.email.usage.repository.TenantQuotaRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UsageAnalyticsService {

  private final DailyUsageAggregateRepository aggregateRepository;
  private final TenantQuotaRepository quotaRepository;
  private final Clock clock;

  public UsageAnalyticsService(
      DailyUsageAggregateRepository aggregateRepository,
      TenantQuotaRepository quotaRepository,
      Clock clock) {
    this.aggregateRepository = aggregateRepository;
    this.quotaRepository = quotaRepository;
    this.clock = clock;
  }

  public UsageSummary summarize(String tenantId, LocalDate from, LocalDate to) {
    List<DailyUsageAggregate> aggregates =
        aggregateRepository.findByTenantIdAndUsageDateBetweenOrderByUsageDate(tenantId, from, to);
    long sent = aggregates.stream().mapToLong(DailyUsageAggregate::getSentCount).sum();
    long delivered = aggregates.stream().mapToLong(DailyUsageAggregate::getDeliveredCount).sum();
    long bounced = aggregates.stream().mapToLong(DailyUsageAggregate::getBouncedCount).sum();
    long opened = aggregates.stream().mapToLong(DailyUsageAggregate::getOpenedCount).sum();
    long clicked = aggregates.stream().mapToLong(DailyUsageAggregate::getClickedCount).sum();
    long spamComplaints =
        aggregates.stream().mapToLong(DailyUsageAggregate::getSpamComplaintCount).sum();

    double deliveryRate = sent == 0 ? 0 : (double) delivered / sent;
    double bounceRate = sent == 0 ? 0 : (double) bounced / sent;
    double openRate = delivered == 0 ? 0 : (double) opened / delivered;

    return new UsageSummary(
        tenantId,
        from,
        to,
        sent,
        delivered,
        bounced,
        opened,
        clicked,
        spamComplaints,
        deliveryRate,
        bounceRate,
        openRate);
  }

  public List<UsageTrendPoint> trend(String tenantId, LocalDate from, LocalDate to) {
    return aggregateRepository
        .findByTenantIdAndUsageDateBetweenOrderByUsageDate(tenantId, from, to)
        .stream()
        .map(
            aggregate ->
                new UsageTrendPoint(
                    aggregate.getUsageDate(),
                    aggregate.getSentCount(),
                    aggregate.getDeliveredCount(),
                    aggregate.getBouncedCount(),
                    aggregate.getOpenedCount(),
                    aggregate.getSpamComplaintCount()))
        .collect(Collectors.toList());
  }

  public QuotaStatus quotaStatus(String tenantId) {
    LocalDate today = LocalDate.now(clock);
    YearMonth currentMonth = YearMonth.from(today);
    LocalDate monthStart = currentMonth.atDay(1);
    TenantQuota quota =
        quotaRepository
            .findByTenantId(tenantId)
            .orElseGet(() -> defaultQuota(tenantId, today));

    long monthlyUsage = aggregateRepository.sumQuotaBetween(tenantId, monthStart, today);
    long todaysUsage =
        aggregateRepository
            .findByTenantIdAndUsageDate(tenantId, today)
            .map(DailyUsageAggregate::getQuotaConsumed)
            .orElse(0L);

    boolean quotaExceeded = monthlyUsage >= quota.getMonthlyQuota();
    boolean burstExceeded = todaysUsage >= quota.getDailyBurstLimit();

    return new QuotaStatus(
        tenantId,
        quota.getMonthlyQuota(),
        monthlyUsage,
        quota.getDailyBurstLimit(),
        todaysUsage,
        quota.getAlertThresholdPercent(),
        quotaExceeded,
        burstExceeded);
  }

  public List<AnomalyAlert> detectAnomalies(String tenantId) {
    LocalDate today = LocalDate.now(clock);
    LocalDate twoWeeksAgo = today.minusDays(14);
    List<DailyUsageAggregate> aggregates =
        aggregateRepository.findByTenantIdAndUsageDateBetweenOrderByUsageDate(
            tenantId, twoWeeksAgo, today);

    if (aggregates.isEmpty()) {
      return List.of();
    }

    Optional<DailyUsageAggregate> latest =
        aggregates.stream()
            .max((a, b) -> a.getUsageDate().compareTo(b.getUsageDate()));

    if (latest.isEmpty()) {
      return List.of();
    }

    double latestBounceRate = rate(latest.get().getBouncedCount(), latest.get().getSentCount());
    double baselineBounce =
        aggregates.stream()
            .filter(it -> !it.getUsageDate().equals(latest.get().getUsageDate()))
            .mapToDouble(it -> rate(it.getBouncedCount(), it.getSentCount()))
            .average()
            .orElse(0);

    if (latestBounceRate > baselineBounce * 1.5 && latestBounceRate > 0.05) {
      return List.of(
          new AnomalyAlert(
              tenantId,
              latest.get().getUsageDate(),
              "Bounce rate spike",
              latestBounceRate,
              baselineBounce));
    }

    return List.of();
  }

  public List<UsageReportRow> dailyReport(LocalDate usageDate) {
    return aggregateRepository.findAll().stream()
        .filter(agg -> usageDate.equals(agg.getUsageDate()))
        .map(
            agg ->
                new UsageReportRow(
                    agg.getTenantId(),
                    agg.getUsageDate(),
                    agg.getSentCount(),
                    agg.getDeliveredCount(),
                    agg.getBouncedCount(),
                    agg.getOpenedCount(),
                    agg.getSpamComplaintCount(),
                    agg.getQuotaConsumed()))
        .toList();
  }

  private static double rate(long numerator, long denominator) {
    if (denominator == 0) {
      return 0;
    }
    return (double) numerator / denominator;
  }

  private TenantQuota defaultQuota(String tenantId, LocalDate today) {
    return new TenantQuota(null, tenantId, 100_000, 10_000, 80, today.atStartOfDay(clock.getZone()).toInstant());
  }
}
