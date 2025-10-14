package com.ejada.gateway.graphql;

import com.ejada.gateway.aggregate.DownstreamAggregationClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.Spliterators;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service backing the GraphQL schema. Performs downstream calls and maps JSON responses into
 * strongly typed GraphQL nodes.
 */
@Service
public class TenantGraphQlService {

  private final DownstreamAggregationClient downstreamClient;

  public TenantGraphQlService(DownstreamAggregationClient downstreamClient) {
    this.downstreamClient = Objects.requireNonNull(downstreamClient, "downstreamClient");
  }

  public Mono<TenantNode> fetchTenant(Integer tenantId) {
    return downstreamClient.fetchTenantProfile(tenantId)
        .map(this::mapTenant);
  }

  public Mono<List<TenantNode>> fetchTenants(List<Integer> tenantIds) {
    return Flux.fromIterable(tenantIds)
        .flatMap(this::fetchTenant)
        .collectList();
  }

  public Mono<Map<Integer, List<SubscriptionNode>>> fetchSubscriptions(Set<Integer> tenantIds) {
    return downstreamClient.fetchTenantSubscriptionsBatch(tenantIds)
        .map(map -> map.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                entry -> mapSubscriptions(entry.getValue()))));
  }

  public Mono<Map<Integer, List<CatalogItemNode>>> fetchCatalogEntries(Set<Integer> tenantIds) {
    return downstreamClient.fetchCatalogBatch(tenantIds)
        .map(map -> map.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                entry -> mapCatalog(entry.getValue()))));
  }

  public Mono<Map<Integer, BillingSummaryNode>> fetchBillingSummaries(Set<Integer> tenantIds) {
    return downstreamClient.fetchBillingSummaryBatch(tenantIds)
        .map(map -> map.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                entry -> mapBilling(entry.getValue()))));
  }

  private TenantNode mapTenant(JsonNode node) {
    if (node == null || node.isNull()) {
      return new TenantNode(null, null, null, null);
    }
    return new TenantNode(node.path("id").asInt(),
        text(node, "code"),
        text(node, "name"),
        text(node, "status"));
  }

  private List<SubscriptionNode> mapSubscriptions(JsonNode node) {
    if (node == null || node.isNull()) {
      return List.of();
    }
    if (node.has("items") && node.get("items").isArray()) {
      node = node.get("items");
    }
    JsonNode iterable = node.isArray() ? node : node.path("data");
    if (iterable == null || iterable.isMissingNode() || !iterable.isArray()) {
      return List.of();
    }
    return toStream(iterable)
        .map(item -> new SubscriptionNode(text(item, "id"),
            text(item, "status"),
            text(item, "product"),
            item.path("seats").asInt(),
            text(item, "startDate")))
        .toList();
  }

  private List<CatalogItemNode> mapCatalog(JsonNode node) {
    if (node == null || node.isNull()) {
      return List.of();
    }
    JsonNode iterable = node.has("items") ? node.get("items") : node;
    if (iterable == null || iterable.isMissingNode() || !iterable.isArray()) {
      return List.of();
    }
    return toStream(iterable)
        .map(item -> new CatalogItemNode(text(item, "id"),
            text(item, "name"),
            text(item, "category"),
            text(item, "description")))
        .toList();
  }

  private BillingSummaryNode mapBilling(JsonNode node) {
    if (node == null || node.isNull()) {
      return new BillingSummaryNode("USD", 0.0, 0.0, null);
    }
    return new BillingSummaryNode(text(node, "currency", "USD"),
        node.path("totalDue").asDouble(node.path("monthlySpend").asDouble(0.0)),
        node.path("usagePercentage").asDouble(),
        text(node, "nextInvoiceDate"));
  }

  private String text(JsonNode node, String field) {
    return text(node, field, null);
  }

  private String text(JsonNode node, String field, String defaultValue) {
    if (node == null || node.isNull() || field == null) {
      return defaultValue;
    }
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() ? defaultValue : value.asText();
  }

  private Stream<JsonNode> toStream(JsonNode array) {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(array.elements(), 0), false);
  }
}
