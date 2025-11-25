package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

class JwtTenantFilter extends OncePerRequestFilter {

  private final String tenantClaim;

  JwtTenantFilter(String tenantClaim) {
    this.tenantClaim = tenantClaim;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
    try {
      propagateTenant(response);
      filterChain.doFilter(request, response);
    } finally {
      ContextManager.Tenant.clear();
    }
  }

  private void propagateTenant(HttpServletResponse response) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      return;
    }

    Jwt jwt = jwtAuth.getToken();
    Object tenantFromClaim = jwt.getClaims().get(tenantClaim);
    if (tenantFromClaim == null) {
      return;
    }

    String tenantId = String.valueOf(tenantFromClaim);
    ContextManager.Tenant.set(tenantId);
    response.setHeader(HeaderNames.X_TENANT_ID, tenantId);
  }
}
