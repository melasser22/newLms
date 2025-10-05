package com.ejada.analytics.service;

import com.ejada.analytics.dto.AnalyticsPeriod;
import com.ejada.analytics.dto.CostForecastResponse;
import com.ejada.analytics.dto.FeatureAdoptionResponse;
import com.ejada.analytics.dto.FeatureUsageSummaryDto;
import com.ejada.analytics.dto.PeakUsageWindowDto;
import com.ejada.analytics.dto.UsageSummaryResponse;
import com.ejada.analytics.dto.UsageTrendPointDto;
import com.ejada.analytics.model.FeatureUsageDailyView;
import com.ejada.analytics.model.FeatureUsageDailyViewId;
import com.ejada.analytics.model.PeakUsageHourView;
import com.ejada.analytics.model.PeakUsageHourViewId;
import com.ejada.analytics.model.UsageSummaryView;
import com.ejada.analytics.model.UsageSummaryViewId;
import com.ejada.analytics.repository.FeatureUsageDailyViewRepository;
import com.ejada.analytics.repository.PeakUsageHourViewRepository;
import com.ejada.analytics.repository.UsageSummaryViewRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

  private static final int DEFAULT_FEATURE_LIMIT = 10;
  private static final int FORECAST_HORIZON_DAYS = 30;
  private static final int FORECAST_LOOKBACK_DAYS = 90;

  private final UsageSummaryViewRepository usageSummaryViewRepository;
  private final FeatureUsageDailyViewRepository featureUsageDailyViewRepository;
  private final PeakUsageHourViewRepository peakUsageHourViewRepository;

  public AnalyticsService(
      UsageSummaryViewRepository usageSummaryViewRepository,
      FeatureUsageDailyViewRepository featureUsageDailyViewRepository,
      PeakUsageHourViewRepository peakUsageHourViewRepository) {
    this.usageSummaryViewRepository = usageSummaryViewRepository;
    this.featureUsageDailyViewRepository = featureUsageDailyViewRepository;
    this.peakUsageHourViewRepository = peakUsageHourViewRepository;
  }

  @Cacheable(cacheNames = "analytics:usage-summary", key = "#tenantId + ':' + #period")
  public UsageSummaryResponse getUsageSummary(Long tenantId, AnalyticsPeriod period) {
    DateRange range = resolveRange(period);
    List<UsageSummaryView> summaries =
        usageSummaryViewRepository.findForTenantAndPeriod(tenantId, range.start(), range.end());

    Map<String, List<PeakUsageWindowDto>> peakWindows = buildPeakWindows(tenantId, range);
    Map<String, List<FeatureUsageDailyView>> usageDailyByFeature =
        loadDailyUsage(tenantId, range, FORECAST_LOOKBACK_DAYS).stream()
            .filter(view -> view.getId() != null && view.getId().getFeatureKey() != null)
            .collect(Collectors.groupingBy(v -> requireFeatureKey(v.getId())));

    List<FeatureUsageSummaryDto> features =
        summaries.stream()
            .filter(summary -> summary.getId() != null && summary.getId().getFeatureKey() != null)
            .sorted(Comparator.comparing(v -> requireFeatureKey(v.getId())))
            .limit(DEFAULT_FEATURE_LIMIT)
            .map(
                view -> {
                  UsageSummaryViewId id = requireUsageSummaryId(view);
                  String featureKey = requireFeatureKey(id);
                  return buildFeatureSummary(
                      view,
                      peakWindows.getOrDefault(featureKey, List.of()),
                      usageDailyByFeature.getOrDefault(featureKey, List.of()));
                })
            .toList();

    return new UsageSummaryResponse(tenantId, period, range.start(), range.end(), features);
  }

  @Cacheable(cacheNames = "analytics:feature-adoption", key = "#tenantId")
  public FeatureAdoptionResponse getFeatureAdoption(Long tenantId) {
    OffsetDateTime end = nowUtc();
    OffsetDateTime start = end.minusDays(60);
    List<FeatureUsageDailyView> usage =
        featureUsageDailyViewRepository.findForTenantBetween(tenantId, start, end);

    Map<String, List<UsageTrendPointDto>> trendByFeature =
        usage.stream()
            .filter(
                view ->
                    view.getId() != null
                        && view.getId().getFeatureKey() != null
                        && view.getId().getUsageDay() != null)
            .collect(
                Collectors.groupingBy(
                    v -> requireFeatureKey(v.getId()),
                    Collectors.collectingAndThen(
                        Collectors.mapping(
                            v ->
                                new UsageTrendPointDto(
                                    requireUsageDay(v.getId()),
                                    safeBigDecimal(v.getTotalUsage()),
                                    Optional.ofNullable(v.getEventCount()).orElse(0L)),
                            Collectors.toList()),
                        list ->
                            list.stream()
                                .sorted(Comparator.comparing(UsageTrendPointDto::timestamp))
                                .toList())));

    List<FeatureAdoptionResponse.FeatureTrendDto> features =
        trendByFeature.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new FeatureAdoptionResponse.FeatureTrendDto(e.getKey(), e.getValue()))
            .toList();

    return new FeatureAdoptionResponse(tenantId, features);
  }

  @Cacheable(cacheNames = "analytics:cost-forecast", key = "#tenantId")
  public CostForecastResponse getCostForecast(Long tenantId) {
    OffsetDateTime end = nowUtc();
    OffsetDateTime start = end.minusDays(FORECAST_LOOKBACK_DAYS);

    List<FeatureUsageDailyView> usage =
        featureUsageDailyViewRepository.findForTenantBetween(tenantId, start, end);

    Map<String, List<FeatureUsageDailyView>> byFeature =
        usage.stream()
            .filter(
                view ->
                    view.getId() != null
                        && view.getId().getFeatureKey() != null
                        && view.getId().getUsageDay() != null)
            .collect(
                Collectors.groupingBy(
                    v -> requireFeatureKey(v.getId()),
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        list ->
                            list.stream()
                                .sorted(Comparator.comparing(v -> requireUsageDay(v.getId())))
                                .toList())));

    List<CostForecastResponse.FeatureForecastDto> forecasts = new ArrayList<>();
    byFeature.forEach(
        (feature, points) -> {
          BigDecimal currentUsage =
              points.stream()
                  .filter(v -> requireUsageDay(v.getId()).isAfter(end.minusDays(30)))
                  .map(v -> safeBigDecimal(v.getTotalUsage()))
                  .reduce(BigDecimal.ZERO, BigDecimal::add);
          BigDecimal planLimit =
              points.stream()
                  .map(FeatureUsageDailyView::getPlanLimit)
                  .filter(Objects::nonNull)
                  .max(Comparator.naturalOrder())
                  .orElse(null);
          BigDecimal forecasted = forecastUsage(points, FORECAST_HORIZON_DAYS);
          boolean overageRisk = exceedsPlanLimit(forecasted, planLimit);
          List<String> recommendations = buildForecastRecommendations(currentUsage, forecasted, planLimit);

          forecasts.add(
              new CostForecastResponse.FeatureForecastDto(
                  feature, currentUsage, forecasted, planLimit, overageRisk, recommendations));
        });

    return new CostForecastResponse(
        tenantId,
        forecasts.stream()
            .sorted(Comparator.comparing(feature -> feature.featureKey()))
            .toList(),
        new CostForecastResponse.OffsetDateTimeRange(end, end.plusDays(FORECAST_HORIZON_DAYS)));
  }

  private FeatureUsageSummaryDto buildFeatureSummary(
      UsageSummaryView view,
      List<PeakUsageWindowDto> peakUsage,
      List<FeatureUsageDailyView> usageHistory) {
    UsageSummaryViewId id = requireUsageSummaryId(view);
    String featureKey = requireFeatureKey(id);
    BigDecimal totalUsage = safeBigDecimal(view.getTotalUsage());
    Long eventCount = Optional.ofNullable(view.getEventCount()).orElse(0L);
    BigDecimal planLimit = view.getPlanLimit();
    BigDecimal utilization = calculateUtilization(totalUsage, planLimit);
    BigDecimal forecast = forecastUsage(usageHistory, FORECAST_HORIZON_DAYS);
    boolean overageRisk = exceedsPlanLimit(forecast, planLimit);
    List<String> recommendations = buildForecastRecommendations(totalUsage, forecast, planLimit);

    return new FeatureUsageSummaryDto(
        featureKey,
        totalUsage,
        eventCount,
        planLimit,
        utilization,
        forecast,
        overageRisk,
        recommendations,
        peakUsage);
  }

  private Map<String, List<PeakUsageWindowDto>> buildPeakWindows(Long tenantId, DateRange range) {
    OffsetDateTime peakStart = range.start().minusDays(7);
    List<PeakUsageHourView> peakUsage =
        peakUsageHourViewRepository.findForTenantBetween(tenantId, peakStart, range.end());

    return peakUsage.stream()
        .filter(
            view ->
                view.getId() != null
                    && view.getId().getFeatureKey() != null
                    && view.getId().getUsageHour() != null)
        .collect(
            Collectors.groupingBy(
                v -> requireFeatureKey(v.getId()),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list ->
                        list.stream()
                            .sorted(Comparator.comparing(PeakUsageHourView::getEventCount).reversed())
                            .limit(3)
                            .map(
                                v ->
                                    new PeakUsageWindowDto(
                                        requireUsageHour(v.getId()),
                                        Optional.ofNullable(v.getEventCount()).orElse(0L),
                                        safeBigDecimal(v.getTotalUsage())))
                            .toList())));
  }

  private List<FeatureUsageDailyView> loadDailyUsage(
      Long tenantId, DateRange range, int additionalLookbackDays) {
    OffsetDateTime start = range.start().minusDays(additionalLookbackDays);
    return featureUsageDailyViewRepository.findForTenantBetween(tenantId, start, range.end());
  }

  private BigDecimal forecastUsage(List<FeatureUsageDailyView> usageHistory, int horizonDays) {
    if (usageHistory.isEmpty()) {
      return BigDecimal.ZERO;
    }
    List<FeatureUsageDailyView> ordered =
        usageHistory.stream()
            .filter(view -> view.getId() != null && view.getId().getUsageDay() != null)
            .sorted(Comparator.comparing(v -> requireUsageDay(v.getId())))
            .toList();

    if (ordered.isEmpty()) {
      return BigDecimal.ZERO;
    }

    int n = ordered.size();
    double sumX = 0;
    double sumY = 0;
    double sumXX = 0;
    double sumXY = 0;
    for (int i = 0; i < n; i++) {
      double x = i;
      double y = safeBigDecimal(ordered.get(i).getTotalUsage()).doubleValue();
      sumX += x;
      sumY += y;
      sumXX += x * x;
      sumXY += x * y;
    }
    double denominator = (n * sumXX) - (sumX * sumX);
    double slope = denominator == 0 ? 0 : ((n * sumXY) - (sumX * sumY)) / denominator;
    double intercept = (sumY - (slope * sumX)) / n;

    double latestDaily = safeBigDecimal(ordered.get(n - 1).getTotalUsage()).doubleValue();
    double projectedDaily = Math.max(intercept + slope * n, latestDaily);
    double forecast = projectedDaily * horizonDays;
    return BigDecimal.valueOf(forecast).setScale(2, RoundingMode.HALF_UP);
  }

  private List<String> buildForecastRecommendations(
      BigDecimal currentUsage, BigDecimal forecastedUsage, BigDecimal planLimit) {
    List<String> recommendations = new ArrayList<>();
    if (exceedsPlanLimit(forecastedUsage, planLimit)) {
      recommendations.add("Forecast exceeds plan limit; consider upgrading or purchasing add-ons.");
    }
    if (planLimit != null && planLimit.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal utilization = calculateUtilization(currentUsage, planLimit);
      if (utilization.compareTo(BigDecimal.valueOf(50)) < 0) {
        recommendations.add("Utilization below 50%; evaluate downgrading to reduce costs.");
      }
    }
    if (recommendations.isEmpty()) {
      recommendations.add("Maintain current plan; usage is within optimal thresholds.");
    }
    return recommendations;
  }

  private boolean exceedsPlanLimit(BigDecimal forecastedUsage, BigDecimal planLimit) {
    return planLimit != null
        && planLimit.compareTo(BigDecimal.ZERO) > 0
        && forecastedUsage.compareTo(planLimit) > 0;
  }

  private BigDecimal calculateUtilization(BigDecimal usage, BigDecimal planLimit) {
    if (planLimit == null || planLimit.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }
    return usage
        .multiply(BigDecimal.valueOf(100))
        .divide(planLimit, 2, RoundingMode.HALF_UP)
        .min(BigDecimal.valueOf(999));
  }

  private BigDecimal safeBigDecimal(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private UsageSummaryViewId requireUsageSummaryId(UsageSummaryView view) {
    return Objects.requireNonNull(view.getId(), "Usage summary row is missing its identifier");
  }

  private String requireFeatureKey(UsageSummaryViewId id) {
    return Objects.requireNonNull(id.getFeatureKey(), "Usage summary identifier lacks feature key");
  }

  private String requireFeatureKey(FeatureUsageDailyViewId id) {
    return Objects.requireNonNull(id.getFeatureKey(), "Daily usage identifier lacks feature key");
  }

  private String requireFeatureKey(PeakUsageHourViewId id) {
    return Objects.requireNonNull(id.getFeatureKey(), "Peak usage identifier lacks feature key");
  }

  private OffsetDateTime requireUsageDay(FeatureUsageDailyViewId id) {
    return Objects.requireNonNull(id.getUsageDay(), "Daily usage identifier lacks usage day");
  }

  private OffsetDateTime requireUsageHour(PeakUsageHourViewId id) {
    return Objects.requireNonNull(id.getUsageHour(), "Peak usage identifier lacks usage hour");
  }

  private DateRange resolveRange(AnalyticsPeriod period) {
    OffsetDateTime now = nowUtc();
    return switch (period) {
      case DAILY -> new DateRange(now.minusDays(1), now);
      case WEEKLY -> new DateRange(now.minusDays(7), now);
      case MONTHLY -> {
        OffsetDateTime start = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime end = start.plusMonths(1);
        yield new DateRange(start, end);
      }
    };
  }

  private OffsetDateTime nowUtc() {
    return OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
  }

  private record DateRange(OffsetDateTime start, OffsetDateTime end) {}
}
