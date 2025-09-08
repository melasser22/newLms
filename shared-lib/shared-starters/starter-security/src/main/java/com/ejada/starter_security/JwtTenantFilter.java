package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Propagates tenant information from the JWT claim into the {@link ContextManager}
 * and echoes it back as {@code X-Tenant-Id} response header.
 */
class JwtTenantFilter implements WebFilter {

    private final String tenantClaim;

    JwtTenantFilter(String tenantClaim) {
        this.tenantClaim = tenantClaim;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return exchange.getPrincipal()
                .flatMap(principal -> {
                    if (principal instanceof Authentication auth && auth instanceof JwtAuthenticationToken jwtAuth) {
                        Jwt jwt = jwtAuth.getToken();
                        Object tid = jwt.getClaims().get(tenantClaim);
                        if (tid != null) {
                            String tenant = String.valueOf(tid);
                            ContextManager.Tenant.set(tenant);
                            exchange.getResponse().getHeaders().set(HeaderNames.X_TENANT_ID, tenant);
                        }
                    }
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange))
                .doFinally(signal -> ContextManager.Tenant.clear());
    }
}
