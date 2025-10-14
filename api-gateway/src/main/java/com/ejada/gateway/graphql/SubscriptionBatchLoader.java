package com.ejada.gateway.graphql;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import org.dataloader.MappedBatchLoader;
import org.springframework.stereotype.Component;

/**
 * DataLoader batch loader for subscriptions keyed by tenant identifier.
 */
@Component
public class SubscriptionBatchLoader implements MappedBatchLoader<Integer, List<SubscriptionNode>> {

  public static final String NAME = "subscriptionLoader";

  private final TenantGraphQlService service;

  public SubscriptionBatchLoader(TenantGraphQlService service) {
    this.service = service;
  }

  @Override
  public CompletionStage<Map<Integer, List<SubscriptionNode>>> load(Set<Integer> keys) {
    return service.fetchSubscriptions(keys).toFuture();
  }
}
