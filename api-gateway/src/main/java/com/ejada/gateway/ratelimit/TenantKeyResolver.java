package com.ejada.gateway.ratelimit;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.gateway.context.GatewayRequestAttributes;
import java.util.Locale;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

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
    return Mono.deferContextual(ctx -> Mono.just(resolveTenant(exchange, ctx)));
  }

  private String resolveTenant(ServerWebExchange exchange, ContextView ctx) {
    String tenant = null;
    if (ctx != null) {
      tenant = trimToNull(ctx.getOrDefault(HeaderNames.X_TENANT_ID, null));
    }
    if (!StringUtils.hasText(tenant)) {
      tenant = trimToNull(exchange.getAttribute(GatewayRequestAttributes.TENANT_ID));
    }
    if (!StringUtils.hasText(tenant)) {
      tenant = trimToNull(ContextManager.Tenant.get());
    }
    if (!StringUtils.hasText(tenant)) {
      tenant = trimToNull(exchange.getRequest().getHeaders().getFirst(HeaderNames.X_TENANT_ID));
    }
    if (!StringUtils.hasText(tenant)) {
      tenant = "public";
    }
    return tenant.toLowerCase(Locale.ROOT);
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}

