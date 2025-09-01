package com.shared.starter_security;

import com.common.context.ContextManager;
import com.common.constants.HeaderNames;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;

class JwtTenantFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;
	private final String tenantClaim;

    JwtTenantFilter(String tenantClaim) {
        this.tenantClaim = tenantClaim;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                Object tid = jwt.getClaims().get(tenantClaim);
                if (tid != null) {
                        String tenant = String.valueOf(tid);
                        ContextManager.Tenant.set(tenant);
                        response.setHeader(HeaderNames.TENANT_ID, tenant);
                }
            }
            chain.doFilter(request, response);
        } finally {
                ContextManager.Tenant.clear();
        }
    }
}
