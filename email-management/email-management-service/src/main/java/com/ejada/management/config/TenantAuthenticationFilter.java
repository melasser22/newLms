package com.ejada.management.config;

import com.ejada.management.service.AuditLogger;
import com.ejada.management.service.TenantAuthorizationService;
import com.ejada.management.service.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TenantAuthenticationFilter extends OncePerRequestFilter {

  private static final Pattern TENANT_PATTERN = Pattern.compile("/tenants/([^/]+)");

  private final TenantAuthorizationService authorizationService;
  private final AuditLogger auditLogger;

  public TenantAuthenticationFilter(
      TenantAuthorizationService authorizationService, AuditLogger auditLogger) {
    this.authorizationService = authorizationService;
    this.auditLogger = auditLogger;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/actuator") || path.startsWith("/api/v1/webhooks");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String tenantId = resolveTenantId(request);
      if (tenantId == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing tenant id");
      }
      authorizationService.verifyAccess(tenantId, request.getHeader("X-Tenant-Auth"));
      TenantContextHolder.setTenantId(tenantId);
      auditLogger.logTenantAction(tenantId, "REQUEST", request.getMethod() + " " + request.getRequestURI());
      filterChain.doFilter(request, response);
    } finally {
      TenantContextHolder.clear();
    }
  }

  private String resolveTenantId(HttpServletRequest request) {
    String header = request.getHeader("X-Tenant-Id");
    if (StringUtils.hasText(header)) {
      return header;
    }
    Matcher matcher = TENANT_PATTERN.matcher(request.getRequestURI());
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }
}
