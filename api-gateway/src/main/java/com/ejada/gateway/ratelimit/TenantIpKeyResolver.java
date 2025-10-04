package com.ejada.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Combines the tenant and client IP resolvers to provide a more granular
 * rate-limiting key. The resulting key is in the form
 * {@code <tenant-id>:<client-ip>}.
 */
@Component("tenantIpKeyResolver")
public class TenantIpKeyResolver implements KeyResolver {

  private final TenantKeyResolver tenantKeyResolver;
  private final IpKeyResolver ipKeyResolver;

  public TenantIpKeyResolver(TenantKeyResolver tenantKeyResolver, IpKeyResolver ipKeyResolver) {
    this.tenantKeyResolver = tenantKeyResolver;
    this.ipKeyResolver = ipKeyResolver;
  }

  @Override
  public Mono<String> resolve(ServerWebExchange exchange) {
    return Mono.zip(tenantKeyResolver.resolve(exchange), ipKeyResolver.resolve(exchange))
        .map(tuple -> tuple.getT1() + ":" + tuple.getT2());
  }
}
