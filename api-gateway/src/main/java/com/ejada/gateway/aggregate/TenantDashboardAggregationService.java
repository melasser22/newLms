package com.ejada.gateway.aggregate;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Aggregates the tenant dashboard payload across tenant, subscription and billing services
 * providing resilient fan-out/fan-in semantics used by REST and streaming endpoints.
 */
@Service
public class TenantDashboardAggregationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TenantDashboardAggregationService.class);

  private final DownstreamAggregationClient downstreamClient;

  public TenantDashboardAggregationService(DownstreamAggregationClient downstreamClient) {
    this.downstreamClient = Objects.requireNonNull(downstreamClient, "downstreamClient");
  }

  public Mono<TenantDashboardAggregateResponse> aggregate(Integer tenantId) {
    Objects.requireNonNull(tenantId, "tenantId");

    Mono<JsonNode> tenantMono = downstreamClient.fetchTenantProfile(tenantId)
        .onErrorResume(ex -> {
          LOGGER.warn("Tenant lookup failed for {}", tenantId, ex);
          return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Tenant profile unavailable", ex));
        });

    Mono<SafeResult<JsonNode>> subscriptionsMono = safeFetch(
        downstreamClient.fetchTenantSubscriptions(tenantId),
        "Subscription catalogue unavailable");

    Mono<SafeResult<JsonNode>> billingMono = safeFetch(
        downstreamClient.fetchBillingSummary(tenantId),
        "Billing summary unavailable");

    return Mono.zip(tenantMono, subscriptionsMono, billingMono)
        .map(tuple -> {
          JsonNode tenant = tuple.getT1();
          SafeResult<JsonNode> subscriptions = tuple.getT2();
          SafeResult<JsonNode> billing = tuple.getT3();

          List<String> warnings = Stream.of(subscriptions.warning(), billing.warning())
              .filter(StringUtils::hasText)
              .distinct()
              .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

          return new TenantDashboardAggregateResponse(tenant,
              subscriptions.value(),
              billing.value(),
              warnings,
              Instant.now());
        })
        .onErrorResume(ResponseStatusException.class, Mono::error)
        .onErrorResume(ex -> {
          LOGGER.warn("Dashboard aggregation failed for tenant {}", tenantId, ex);
          return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
              "Unable to aggregate tenant dashboard", ex));
        });
  }

  private <T> Mono<SafeResult<T>> safeFetch(Mono<T> mono, String warning) {
    if (mono == null) {
      return Mono.just(SafeResult.warning(warning));
    }
    return mono.map(SafeResult::success)
        .onErrorResume(ex -> {
          LOGGER.warn("Downstream call failed: {}", ex.getMessage());
          return Mono.just(SafeResult.warning(warning));
        });
  }

  private record SafeResult<T>(T value, String warning) {

    static <T> SafeResult<T> success(T value) {
      return new SafeResult<>(value, null);
    }

    static <T> SafeResult<T> warning(String warning) {
      return new SafeResult<>(null, warning);
    }
  }
}
