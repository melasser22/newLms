package com.ejada.gateway.aggregate;

import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.GatewayAggregationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Shared client responsible for orchestrating fan-out calls to downstream services used by the
 * aggregation layer. Centralises WebClient configuration and endpoint conventions so the
 * individual aggregation services can focus on composition logic.
 */
@Component
public class DownstreamAggregationClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(DownstreamAggregationClient.class);

  private static final ParameterizedTypeReference<BaseResponse<JsonNode>> BASE_RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {
      };

  private final WebClient tenantClient;
  private final WebClient subscriptionClient;
  private final WebClient billingClient;
  private final WebClient analyticsClient;
  private final WebClient catalogClient;
  private final WebClient auditClient;
  private final Duration timeout;

  public DownstreamAggregationClient(WebClient.Builder builder, GatewayAggregationProperties properties) {
    Objects.requireNonNull(builder, "builder");
    Objects.requireNonNull(properties, "properties");
    this.tenantClient = builder.clone().baseUrl(properties.getTenantServiceUri().toString()).build();
    this.subscriptionClient = builder.clone().baseUrl(properties.getSubscriptionServiceUri().toString()).build();
    this.billingClient = builder.clone().baseUrl(properties.getBillingServiceUri().toString()).build();
    this.analyticsClient = builder.clone().baseUrl(properties.getAnalyticsServiceUri().toString()).build();
    this.catalogClient = builder.clone().baseUrl(properties.getCatalogServiceUri().toString()).build();
    this.auditClient = builder.clone().baseUrl(properties.getAuditServiceUri().toString()).build();
    this.timeout = properties.getRequestTimeout();
  }

  public Mono<JsonNode> fetchTenantProfile(Integer tenantId) {
    return tenantClient.get()
        .uri("/api/v1/tenants/{tenantId}", tenantId)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(BASE_RESPONSE_TYPE)
        .timeout(timeout)
        .flatMap(response -> {
          if (response == null || !response.isSuccess() || response.getData() == null) {
            return Mono.error(new IllegalStateException("Tenant service returned empty payload"));
          }
          return Mono.just(response.getData());
        });
  }

  public Mono<JsonNode> fetchTenantSubscriptions(Integer tenantId) {
    return subscriptionClient.get()
        .uri("/api/v1/subscriptions/tenants/{tenantId}", tenantId)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .timeout(timeout);
  }

  public Mono<Map<Integer, JsonNode>> fetchTenantSubscriptionsBatch(Set<Integer> tenantIds) {
    if (tenantIds == null || tenantIds.isEmpty()) {
      return Mono.just(Map.of());
    }
    String ids = tenantIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    return subscriptionClient.get()
        .uri(builder -> builder.path("/api/v1/subscriptions/batch")
            .queryParam("tenantIds", ids)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<Integer, JsonNode>>() {
        })
        .timeout(timeout)
        .onErrorResume(ex -> {
          LOGGER.warn("Batch subscription lookup failed: {}", ex.getMessage());
          return Flux.fromIterable(tenantIds)
              .flatMap(id -> fetchTenantSubscriptions(id)
                  .map(node -> Map.entry(id, node))
                  .onErrorResume(err -> Mono.just(Map.entry(id, null))))
              .collectMap(Map.Entry::getKey, Map.Entry::getValue);
        });
  }

  public Mono<JsonNode> fetchBillingSummary(Integer tenantId) {
    return billingClient.get()
        .uri("/billing/tenants/{tenantId}/summary", tenantId)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .timeout(timeout);
  }

  public Mono<JsonNode> fetchBillingConsumption(Long subscriptionId, Long customerId) {
    return billingClient.get()
        .uri(builder -> {
          var uriBuilder = builder.path("/billing/subscriptions/{subscriptionId}/consumption");
          if (customerId != null) {
            uriBuilder.queryParam("customerId", customerId);
          }
          return uriBuilder.build(subscriptionId);
        })
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .timeout(timeout);
  }

  public Mono<Map<Integer, JsonNode>> fetchBillingSummaryBatch(Set<Integer> tenantIds) {
    if (tenantIds == null || tenantIds.isEmpty()) {
      return Mono.just(Map.of());
    }
    String ids = tenantIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    return billingClient.get()
        .uri(builder -> builder.path("/billing/tenants/summary")
            .queryParam("tenantIds", ids)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<Integer, JsonNode>>() {
        })
        .timeout(timeout)
        .onErrorResume(ex -> fallbackBatch(tenantIds, this::fetchBillingSummary));
  }

  public Mono<JsonNode> fetchUsageStatistics(Integer tenantId) {
    return analyticsClient.get()
        .uri("/api/v1/analytics/tenants/{tenantId}/usage-summary", tenantId)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .timeout(timeout);
  }

  public Mono<JsonNode> fetchRealtimeUsage(Integer tenantId) {
    return analyticsClient.get()
        .uri("/api/v1/analytics/tenants/{tenantId}/metrics", tenantId)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .timeout(timeout);
  }

  public Mono<Map<Integer, JsonNode>> fetchUsageBatch(Set<Integer> tenantIds) {
    if (tenantIds == null || tenantIds.isEmpty()) {
      return Mono.just(Map.of());
    }
    String ids = tenantIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    return analyticsClient.get()
        .uri(builder -> builder.path("/api/v1/analytics/tenants/usage-summary")
            .queryParam("tenantIds", ids)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<Integer, JsonNode>>() {
        })
        .timeout(timeout)
        .onErrorResume(ex -> fallbackBatch(tenantIds, this::fetchUsageStatistics));
  }

  public Mono<JsonNode> fetchCatalog(Integer tenantId) {
    return catalogClient.get()
        .uri("/api/v1/catalog/tenants/{tenantId}/items", tenantId)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .timeout(timeout);
  }

  public Mono<Map<Integer, JsonNode>> fetchCatalogBatch(Set<Integer> tenantIds) {
    if (tenantIds == null || tenantIds.isEmpty()) {
      return Mono.just(Map.of());
    }
    String ids = tenantIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    return catalogClient.get()
        .uri(builder -> builder.path("/api/v1/catalog/tenants/batch")
            .queryParam("tenantIds", ids)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<Integer, JsonNode>>() {
        })
        .timeout(timeout)
        .onErrorResume(ex -> fallbackBatch(tenantIds, this::fetchCatalog));
  }

  public Mono<JsonNode> fetchAuditEvents(Integer tenantId) {
    return auditClient.get()
        .uri("/api/v1/audit/tenants/{tenantId}/events", tenantId)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .timeout(timeout);
  }

  private <T> Mono<Map<Integer, JsonNode>> fallbackBatch(Set<Integer> tenantIds,
      java.util.function.Function<Integer, Mono<JsonNode>> fallbackFetcher) {
    return Flux.fromIterable(tenantIds)
        .flatMap(id -> fallbackFetcher.apply(id)
            .map(node -> Map.entry(id, node))
            .onErrorResume(err -> Mono.just(Map.entry(id, null))))
        .collectMap(Map.Entry::getKey, Map.Entry::getValue);
  }
}
