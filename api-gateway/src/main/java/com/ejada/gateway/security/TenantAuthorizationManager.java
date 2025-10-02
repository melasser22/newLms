package com.ejada.gateway.security;

import com.ejada.common.context.ContextManager;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import com.ejada.starter_core.web.FilterSkipUtils;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Ensures that authenticated requests contain a resolved tenant context unless the request
 * matches one of the configured skip patterns. This protects downstream services by preventing
 * cross-tenant access at the gateway edge.
 */
public class TenantAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

  private final CoreAutoConfiguration.CoreProps coreProps;

  public TenantAuthorizationManager(CoreAutoConfiguration.CoreProps coreProps) {
    this.coreProps = coreProps;
  }

  @Override
  public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {
    String path = context.getExchange().getRequest().getPath().pathWithinApplication().value();
    if (FilterSkipUtils.shouldSkip(path, coreProps.getTenant().getSkipPatterns())) {
      return Mono.just(new AuthorizationDecision(true));
    }
    return authentication
        .filter(Authentication::isAuthenticated)
        .map(auth -> new AuthorizationDecision(StringUtils.hasText(ContextManager.Tenant.get())))
        .defaultIfEmpty(new AuthorizationDecision(false));
  }
}
