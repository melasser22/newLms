package com.ejada.gateway.loadbalancer;

import java.util.Optional;

/**
 * Minimal abstraction for retrieving the current tenant identifier. The gateway operates in a
 * reactive environment so the value may originate from thread-local storage populated by filters
 * or from Reactor context propagation.
 */
public interface TenantContext {

  /**
   * @return the current tenant identifier if one is associated with the request being processed
   */
  Optional<String> currentTenantId();
}
