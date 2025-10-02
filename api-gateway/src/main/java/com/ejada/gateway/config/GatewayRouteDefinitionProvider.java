package com.ejada.gateway.config;

import reactor.core.publisher.Flux;

/**
 * SPI that allows the gateway to source additional route definitions from dynamic backends
 * such as configuration services or databases.
 */
public interface GatewayRouteDefinitionProvider {

  /**
   * @return human readable name for logging/metrics.
   */
  default String getProviderName() {
    return getClass().getSimpleName();
  }

  /**
   * Supplies the current list of routes. Implementations should emit a finite sequence and
   * be idempotent as the gateway may invoke the provider multiple times.
   */
  Flux<GatewayRoutesProperties.ServiceRoute> loadRoutes();
}
