package com.ejada.gateway.graphql;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
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

  @SchemaMapping(typeName = "Tenant", field = "subscriptions")
  public CompletableFuture<List<SubscriptionNode>> subscriptions(TenantNode tenant,
      @org.springframework.graphql.data.method.annotation.DataLoader(SubscriptionBatchLoader.NAME)
      DataLoader<Integer, List<SubscriptionNode>> loader) {
    return loader.load(tenant.id());
  }

  @SchemaMapping(typeName = "Tenant", field = "catalog")
  public CompletableFuture<List<CatalogItemNode>> catalog(TenantNode tenant,
      @org.springframework.graphql.data.method.annotation.DataLoader(CatalogBatchLoader.NAME)
      DataLoader<Integer, List<CatalogItemNode>> loader) {
    return loader.load(tenant.id());
  }

  @SchemaMapping(typeName = "Tenant", field = "billing")
  public CompletableFuture<BillingSummaryNode> billing(TenantNode tenant,
      @org.springframework.graphql.data.method.annotation.DataLoader(BillingBatchLoader.NAME)
      DataLoader<Integer, BillingSummaryNode> loader) {
    return loader.load(tenant.id());
  }
}
