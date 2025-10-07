package com.ejada.gateway.loadbalancer;

import java.util.Optional;
import java.util.UUID;

/**
 * {@link TenantContext} implementation backed by the shared {@code ContextManager}. The
 * implementation simply delegates to {@link com.ejada.common.context.TenantContext} while exposing
 * the tenant identifier as a {@link String} to simplify hashing and affinity decisions.
 */
public class ContextManagerTenantContext implements TenantContext {

  @Override
  public Optional<String> currentTenantId() {
    return com.ejada.common.context.TenantContext.get().map(UUID::toString);
  }
}
