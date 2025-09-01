package com.lms.tenant.config;

import com.common.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves tenant identifiers from the incoming request and stores them in the
 * shared {@link TenantContext} for the duration of the request.
 */
@Component
public class TenantResolverFilter extends OncePerRequestFilter {

    private static final Pattern SUBDOMAIN = Pattern.compile("^([a-z0-9-]+)\\.");

    private final JdbcTemplate jdbc;
    private final TenantResolverService resolver;

    public TenantResolverFilter(JdbcTemplate jdbc, TenantResolverService resolver) {
        this.jdbc = jdbc;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String tenantKey = extract(req);
        TenantResolverService.TenantRow tenant = resolver.resolve(tenantKey);
        if (tenant == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "Tenant not found or inactive");
            return;
        }
        UUID tenantId = tenant.id();
        TenantContext.set(tenantId);
        MDC.put("tenant_id", tenantId.toString());
        try {
            jdbc.execute("select set_config('app.current_tenant', '" + tenantId + "', true)");
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
            MDC.remove("tenant_id");
        }
    }

    private String extract(HttpServletRequest req) {
        String host = req.getHeader("host");
        if (host != null) {
            Matcher m = SUBDOMAIN.matcher(host);
            if (m.find()) {
                return m.group(1);
            }
        }
        String hdr = req.getHeader("X-Tenant-ID");
        if (hdr != null && !hdr.isBlank()) {
            return hdr;
        }
        String claim = req.getHeader("X-Auth-Tenant");
        if (claim != null && !claim.isBlank()) {
            return claim;
        }
        throw new IllegalArgumentException("Tenant not resolved");
    }
}
