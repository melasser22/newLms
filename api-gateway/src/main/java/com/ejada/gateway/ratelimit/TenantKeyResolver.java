package com.ejada.gateway.ratelimit;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.gateway.context.GatewayRequestAttributes;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Resolves rate-limiter keys based on the current tenant. The resolver first
 * checks the exchange attributes (populated by {@code TenantExtractionGatewayFilter})
 * and then falls back to the {@link ContextManager} thread-local to support
 * non-reactive integrations.
 */
@Component("tenantKeyResolver")
public class TenantKeyResolver implements KeyResolver {

  @Override
  public Mono<String> resolve(ServerWebExchange exchange) {
    String tenant = exchange.getAttribute(GatewayRequestAttributes.TENANT_ID);
    if (!StringUtils.hasText(tenant)) {
      tenant = ContextManager.Tenant.get();
    }
    if (!StringUtils.hasText(tenant)) {
      tenant = exchange.getRequest().getHeaders().getFirst(HeaderNames.X_TENANT_ID);
    }
    if (!StringUtils.hasText(tenant)) {
      tenant = "public";
    }
    String key = tenant.trim().toLowerCase();
    return Mono.just(key);
  }
}

