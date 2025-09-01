package com.lms.tenant.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantResolverFilter extends OncePerRequestFilter {

    public static final String TENANT_ID_HEADER = "X-Tenant-ID";
    private static final String MDC_TENANT_KEY = "tenant_id";

    private final JdbcTemplate jdbcTemplate;

    public TenantResolverFilter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String tenantIdStr = request.getHeader(TENANT_ID_HEADER);

        if (tenantIdStr != null && !tenantIdStr.isBlank()) {
            try {
                UUID tenantId = UUID.fromString(tenantIdStr);
                TenantContext.setTenantId(tenantId);
                MDC.put(MDC_TENANT_KEY, tenantId.toString());

                // Set the session variable for RLS in PostgreSQL
                jdbcTemplate.execute("SELECT set_config('app.current_tenant', '" + tenantId + "', true)");

                filterChain.doFilter(request, response);
            } catch (IllegalArgumentException e) {
                // Handle invalid UUID format
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Tenant ID format.");
            } finally {
                // Clean up after the request
                TenantContext.clear();
                MDC.remove(MDC_TENANT_KEY);
            }
        } else {
            // Continue without a tenant context for public endpoints
            filterChain.doFilter(request, response);
        }
    }
}
