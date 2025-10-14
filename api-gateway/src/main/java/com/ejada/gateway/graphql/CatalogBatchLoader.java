package com.ejada.gateway.graphql;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import org.dataloader.MappedBatchLoader;
import org.springframework.stereotype.Component;

/**
 * DataLoader batch loader for catalog entries keyed by tenant identifier.
 */
@Component
public class CatalogBatchLoader implements MappedBatchLoader<Integer, List<CatalogItemNode>> {

  public static final String NAME = "catalogLoader";

  private final TenantGraphQlService service;

  public CatalogBatchLoader(TenantGraphQlService service) {
    this.service = service;
  }

  @Override
  public CompletionStage<Map<Integer, List<CatalogItemNode>>> load(Set<Integer> keys) {
    return service.fetchCatalogEntries(keys).toFuture();
  }
}
