package com.ejada.starter_core.tenant;

import com.ejada.common.context.ContextManager;
import com.ejada.starter_core.config.CoreAutoConfiguration.CoreProps;
import com.ejada.starter_core.web.FilterSkipUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @deprecated The tenant responsibilities now live in
 * {@link com.ejada.starter_core.context.ContextFilter} via
 * {@link com.ejada.starter_core.context.TenantContextContributor}. This legacy
 * filter will be removed in a future release.
 */
@Deprecated(since = "1.6.0", forRemoval = true)
public class TenantFilter extends OncePerRequestFilter {

    private final TenantResolver resolver;
    private final CoreProps.Tenant cfg;
    private final String[] skipPatterns;

    // >>> This is the constructor CoreAutoConfiguration calls <<<
    public TenantFilter(TenantResolver resolver, CoreProps.Tenant cfg) {
        this.resolver = resolver;
        this.cfg = cfg;
        this.skipPatterns = FilterSkipUtils.copyOrDefault(cfg.getSkipPatterns());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return FilterSkipUtils.shouldSkip(request.getRequestURI(), skipPatterns);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        TenantResolution resolution = resolver.resolve(req);
        if (resolution.isInvalid()) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid " + cfg.getHeaderName());
            return;
        }

        if (resolution.hasTenant()) {
            String tenant = resolution.tenantId();
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
