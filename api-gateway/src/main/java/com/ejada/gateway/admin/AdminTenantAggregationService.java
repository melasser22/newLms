package com.ejada.gateway.admin;

import com.ejada.gateway.aggregate.DownstreamAggregationClient;
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
 * Aggregates tenant, subscription, analytics and audit data into a single admin oriented payload.
 */
@Service
public class AdminTenantAggregationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdminTenantAggregationService.class);

  private final DownstreamAggregationClient downstreamClient;

  public AdminTenantAggregationService(DownstreamAggregationClient downstreamClient) {
    this.downstreamClient = Objects.requireNonNull(downstreamClient, "downstreamClient");
  }

  public Mono<AdminTenantAggregateResponse> aggregate(Integer tenantId) {
    Objects.requireNonNull(tenantId, "tenantId");

    Mono<JsonNode> tenantMono = downstreamClient.fetchTenantProfile(tenantId)
        .onErrorResume(ex -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
            "Tenant details unavailable", ex)));

    Mono<SafeResult<JsonNode>> subscriptionsMono = safeFetch(
        downstreamClient.fetchTenantSubscriptions(tenantId),
        "Subscriptions unavailable");

    Mono<SafeResult<JsonNode>> usageMono = safeFetch(
        downstreamClient.fetchUsageStatistics(tenantId),
        "Usage statistics unavailable");

    Mono<SafeResult<JsonNode>> auditMono = safeFetch(
        downstreamClient.fetchAuditEvents(tenantId),
        "Audit events unavailable");

    return Mono.zip(tenantMono, subscriptionsMono, usageMono, auditMono)
        .map(tuple -> {
          JsonNode tenant = tuple.getT1();
          SafeResult<JsonNode> subscriptions = tuple.getT2();
          SafeResult<JsonNode> usage = tuple.getT3();
          SafeResult<JsonNode> audit = tuple.getT4();

          List<String> warnings = Stream.of(subscriptions.warning(), usage.warning(), audit.warning())
              .filter(StringUtils::hasText)
              .distinct()
              .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

          return new AdminTenantAggregateResponse(tenant,
              subscriptions.value(),
              usage.value(),
              audit.value(),
              warnings,
              Instant.now());
        })
        .onErrorResume(ResponseStatusException.class, Mono::error)
        .onErrorResume(ex -> {
          LOGGER.warn("Admin tenant aggregation failed for {}", tenantId, ex);
          return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
              "Unable to aggregate tenant profile", ex));
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
}
