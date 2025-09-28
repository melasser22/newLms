package com.ejada.starter_security;

import com.ejada.common.context.ContextManager;
import com.ejada.common.constants.HeaderNames;
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
import java.util.Arrays;
import java.util.Collection;

class JwtTenantFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;
    private final String tenantClaim;

    JwtTenantFilter(String tenantClaim) {
        this.tenantClaim = tenantClaim;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                
                // Check if this is a superadmin token
                Boolean isSuperadmin = jwt.getClaim("isSuperadmin");
                String userId = asString(jwt.getClaims().get("uid"));
                if (userId != null && !userId.isBlank()) {
                    ContextManager.setUserId(userId);
                }

                if (Boolean.TRUE.equals(isSuperadmin)) {
                    // Superadmin doesn't need tenant context
                    response.setHeader("X-Is-Superadmin", "true");
                } else {
                    // Regular tenant user
                    String tenant = asString(jwt.getClaims().get(tenantClaim));
                    if (tenant != null && !tenant.isBlank()) {
                        ContextManager.Tenant.set(tenant);
                        request.setAttribute(HeaderNames.X_TENANT_ID, tenant);
                        response.setHeader(HeaderNames.X_TENANT_ID, tenant);
                    }
                }
            }
            chain.doFilter(request, response);
        } finally {
            ContextManager.Tenant.clear();
            ContextManager.clearUserId();
        }
    }

    private static String asString(Object claim) {
        if (claim == null) {
            return null;
        }
        if (claim instanceof String str) {
            return str;
        }
        if (claim instanceof char[] chars) {
            return new String(chars);
        }
        if (claim instanceof Object[] array) {
            return Arrays.stream(array)
                    .map(JwtTenantFilter::asString)
                    .filter(str -> str != null && !str.isBlank())
                    .findFirst()
                    .orElse(null);
        }
        if (claim instanceof Collection<?> collection) {
            return collection.stream()
                    .map(JwtTenantFilter::asString)
                    .filter(str -> str != null && !str.isBlank())
                    .findFirst()
                    .orElse(null);
        }
        return String.valueOf(claim);
    }
}
