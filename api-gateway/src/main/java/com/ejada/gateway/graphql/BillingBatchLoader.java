package com.ejada.gateway.graphql;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import org.dataloader.MappedBatchLoader;
import org.springframework.stereotype.Component;

/**
 * DataLoader batch loader for billing summaries keyed by tenant identifier.
 */
@Component
public class BillingBatchLoader implements MappedBatchLoader<Integer, BillingSummaryNode> {

  public static final String NAME = "billingLoader";

  private final TenantGraphQlService service;

  public BillingBatchLoader(TenantGraphQlService service) {
    this.service = service;
  }

  @Override
  public CompletionStage<Map<Integer, BillingSummaryNode>> load(Set<Integer> keys) {
    return service.fetchBillingSummaries(keys).toFuture();
  }
}
