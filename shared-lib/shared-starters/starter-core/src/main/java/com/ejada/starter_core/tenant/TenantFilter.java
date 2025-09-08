package com.ejada.starter_core.tenant;

import com.ejada.common.context.ContextManager;
import com.ejada.starter_core.config.CoreAutoConfiguration.CoreProps;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class TenantFilter implements WebFilter, Ordered {

    private final TenantResolver resolver;
    private final CoreProps.Tenant cfg;
    private final AntPathMatcher matcher = new AntPathMatcher();

    // >>> This is the constructor CoreAutoConfiguration calls <<<
    public TenantFilter(TenantResolver resolver, CoreProps.Tenant cfg) {
        this.resolver = resolver;
        this.cfg = cfg;
    }

    @Override
    public int getOrder() {
        return cfg.getOrder();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String uri = exchange.getRequest().getURI().getPath();
        for (String p : cfg.getSkipPatterns()) {
            if (matcher.match(p, uri)) {
                return chain.filter(exchange);
            }
        }

        String tenant = resolver.resolve(exchange);
        if (tenant != null) {
            if (cfg.isEchoResponseHeader()) {
                exchange.getResponse().getHeaders().set(cfg.getHeaderName(), tenant);
            }
            ContextManager.Tenant.set(tenant);
        }

        return chain.filter(exchange).doFinally(s -> ContextManager.Tenant.clear());
    }
}
