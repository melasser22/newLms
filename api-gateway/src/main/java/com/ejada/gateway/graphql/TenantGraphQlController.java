package com.ejada.gateway.graphql;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

/**
 * GraphQL controller that exposes a unified schema across tenant, subscription, catalog and
 * billing domains.
 */
@Controller
public class TenantGraphQlController {

  private final TenantGraphQlService service;

  public TenantGraphQlController(TenantGraphQlService service) {
    this.service = service;
  }

  @QueryMapping
  public Mono<TenantNode> tenant(@Argument Integer id) {
    return service.fetchTenant(id);
  }

  @QueryMapping
  public Mono<List<TenantNode>> tenants(@Argument List<Integer> ids) {
    return service.fetchTenants(ids);
  }

  @BatchMapping(typeName = "Tenant", field = "subscriptions")
  public Mono<Map<TenantNode, List<SubscriptionNode>>> subscriptions(List<TenantNode> tenants) {
    return batchMap(tenants,
        tenantIds -> service.fetchSubscriptions(tenantIds),
        List::of);
  }

  @BatchMapping(typeName = "Tenant", field = "catalog")
  public Mono<Map<TenantNode, List<CatalogItemNode>>> catalog(List<TenantNode> tenants) {
    return batchMap(tenants,
        tenantIds -> service.fetchCatalogEntries(tenantIds),
        List::of);
  }

  @BatchMapping(typeName = "Tenant", field = "billing")
  public Mono<Map<TenantNode, BillingSummaryNode>> billing(List<TenantNode> tenants) {
    return batchMap(tenants,
        tenantIds -> service.fetchBillingSummaries(tenantIds),
        () -> null);
  }

  private <T> Mono<Map<TenantNode, T>> batchMap(List<TenantNode> tenants,
      java.util.function.Function<Set<Integer>, Mono<Map<Integer, T>>> fetcher,
      java.util.function.Supplier<T> defaultSupplier) {
    Set<Integer> tenantIds = tenants.stream()
        .map(TenantNode::id)
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toSet());
    Map<TenantNode, T> defaults = createDefaultMap(tenants, defaultSupplier);
    if (tenantIds.isEmpty()) {
      return Mono.just(defaults);
    }
    return fetcher.apply(tenantIds)
        .map(result -> {
          Map<Integer, T> resolved = result != null ? result : Map.of();
          Map<TenantNode, T> mapped = new LinkedHashMap<>(defaults);
          for (TenantNode tenant : tenants) {
            Integer tenantId = tenant.id();
            if (tenantId != null && resolved.containsKey(tenantId)) {
              mapped.put(tenant, resolved.get(tenantId));
            }
          }
          return mapped;
        })
        .defaultIfEmpty(defaults);
  }

  private <T> Map<TenantNode, T> createDefaultMap(List<TenantNode> tenants,
      java.util.function.Supplier<T> defaultSupplier) {
    Map<TenantNode, T> defaults = new LinkedHashMap<>();
    for (TenantNode tenant : tenants) {
      defaults.put(tenant, defaultSupplier.get());
    }
    return defaults;
  }
}
