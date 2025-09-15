package com.ejada.starter_core.tenant;

import com.ejada.common.context.ContextManager;
import com.ejada.starter_core.config.CoreAutoConfiguration.CoreProps;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TenantFilter extends OncePerRequestFilter {

    private final TenantResolver resolver;
    private final CoreProps.Tenant cfg;
    private final AntPathMatcher matcher = new AntPathMatcher();

    // >>> This is the constructor CoreAutoConfiguration calls <<<
    public TenantFilter(TenantResolver resolver, CoreProps.Tenant cfg) {
        this.resolver = resolver;
        this.cfg = cfg;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String p : cfg.getSkipPatterns()) {
            if (matcher.match(p, uri)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String tenant = resolver.resolve(req);
        if (tenant != null) {
            if (cfg.isEchoResponseHeader()) {
                res.setHeader(cfg.getHeaderName(), tenant);
            }
            try (ContextManager.Tenant.Scope ignored = ContextManager.Tenant.openScope(tenant)) {
                chain.doFilter(req, res);
            }
        } else {
            chain.doFilter(req, res);
        }
    }
}
