package com.ejada.gateway.aggregate;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Produces real-time dashboard snapshots by polling downstream services on a fixed interval and
 * combining the results into a single payload suitable for Server-Sent Events (SSE) streaming.
 */
@Service
public class RealTimeDashboardService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RealTimeDashboardService.class);

  private final TenantDashboardAggregationService dashboardAggregationService;
  private final DownstreamAggregationClient downstreamClient;

  public RealTimeDashboardService(TenantDashboardAggregationService dashboardAggregationService,
      DownstreamAggregationClient downstreamClient) {
    this.dashboardAggregationService = Objects.requireNonNull(dashboardAggregationService, "dashboardAggregationService");
    this.downstreamClient = Objects.requireNonNull(downstreamClient, "downstreamClient");
  }

  public Flux<RealTimeSnapshot> stream(Integer tenantId) {
    Objects.requireNonNull(tenantId, "tenantId");

    return Flux.interval(Duration.ZERO, Duration.ofSeconds(5))
        .flatMap(tick -> aggregateSnapshot(tenantId)
            .onErrorResume(ex -> {
              LOGGER.warn("Realtime snapshot aggregation failed for tenant {}", tenantId, ex);
              return Mono.just(RealTimeSnapshot.failure(ex.getMessage()));
            }))
        .distinctUntilChanged();
  }

  private Mono<RealTimeSnapshot> aggregateSnapshot(Integer tenantId) {
    Mono<TenantDashboardAggregateResponse> dashboardMono = dashboardAggregationService.aggregate(tenantId);

    Mono<SafeResult<JsonNode>> usageMono = safeFetch(downstreamClient.fetchRealtimeUsage(tenantId),
        "Usage metrics unavailable");

    Mono<SafeResult<JsonNode>> billingMono = safeFetch(downstreamClient.fetchBillingSummary(tenantId),
        "Billing metrics unavailable");

    return Mono.zip(dashboardMono, usageMono, billingMono)
        .map(tuple -> {
          TenantDashboardAggregateResponse dashboard = tuple.getT1();
          SafeResult<JsonNode> usage = tuple.getT2();
          SafeResult<JsonNode> billing = tuple.getT3();

          List<String> warnings = Stream.of(dashboard.warnings().stream(),
                  Stream.of(usage.warning(), billing.warning()))
              .flatMap(stream -> stream)
              .filter(StringUtils::hasText)
              .distinct()
              .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

          return RealTimeSnapshot.success(dashboard, usage.value(), billing.value(), warnings, Instant.now());
        });
  }

  private <T> Mono<SafeResult<T>> safeFetch(Mono<T> mono, String warning) {
    return mono.map(SafeResult::success)
        .onErrorResume(ex -> Mono.just(SafeResult.warning(warning)));
  }

  private record SafeResult<T>(T value, String warning) {

    static <T> SafeResult<T> success(T value) {
      return new SafeResult<>(value, null);
    }

    static <T> SafeResult<T> warning(String warning) {
      return new SafeResult<>(null, warning);
    }
  }

  /**
   * Snapshot emitted to SSE clients. Contains aggregated tenant dashboard information plus live
   * usage/billing metrics with consolidated warnings when fallbacks were applied.
   */
  public record RealTimeSnapshot(TenantDashboardAggregateResponse dashboard,
                                 JsonNode usageMetrics,
                                 JsonNode billingMetrics,
                                 List<String> warnings,
                                 Instant generatedAt,
                                 boolean healthy) {

    public RealTimeSnapshot {
      warnings = (warnings == null) ? List.of() : List.copyOf(warnings);
    }

    static RealTimeSnapshot success(TenantDashboardAggregateResponse dashboard,
        JsonNode usage,
        JsonNode billing,
        List<String> warnings,
        Instant generatedAt) {
      return new RealTimeSnapshot(dashboard, usage, billing, warnings, generatedAt, true);
    }

    static RealTimeSnapshot failure(String warning) {
      return new RealTimeSnapshot(null, null, null,
          warning == null ? List.of("Realtime dashboard unavailable") : List.of(warning),
          Instant.now(), false);
    }
  }
}
