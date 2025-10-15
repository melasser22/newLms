package com.ejada.gateway.graphql;

import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderRegistry;
import graphql.GraphQLContext;
import org.springframework.graphql.execution.DataLoaderRegistrar;
import org.springframework.stereotype.Component;

/**
 * Registers GraphQL DataLoaders ensuring the registry is initialised per request.
 */
@Component
public class GatewayDataLoaderRegistrar implements DataLoaderRegistrar {

  private final SubscriptionBatchLoader subscriptionBatchLoader;
  private final CatalogBatchLoader catalogBatchLoader;
  private final BillingBatchLoader billingBatchLoader;

  public GatewayDataLoaderRegistrar(SubscriptionBatchLoader subscriptionBatchLoader,
      CatalogBatchLoader catalogBatchLoader,
      BillingBatchLoader billingBatchLoader) {
    this.subscriptionBatchLoader = subscriptionBatchLoader;
    this.catalogBatchLoader = catalogBatchLoader;
    this.billingBatchLoader = billingBatchLoader;
  }

  @Override
  public void registerDataLoaders(DataLoaderRegistry registry, GraphQLContext context) {
    registry.register(SubscriptionBatchLoader.NAME,
        DataLoaderFactory.newMappedDataLoader(subscriptionBatchLoader));
    registry.register(CatalogBatchLoader.NAME,
        DataLoaderFactory.newMappedDataLoader(catalogBatchLoader));
    registry.register(BillingBatchLoader.NAME,
        DataLoaderFactory.newMappedDataLoader(billingBatchLoader));
  }
}
