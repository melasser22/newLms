package com.lms.tenant.config;
import jakarta.servlet.*; import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException; import java.util.regex.*;
@Component
public class TenantResolverFilter extends OncePerRequestFilter {
  private static final Pattern SUBDOMAIN = Pattern.compile("^([a-z0-9-]+)\\.");
  private final JdbcTemplate jdbc; private final TenantResolverService resolver;
  public TenantResolverFilter(JdbcTemplate jdbc, TenantResolverService resolver){this.jdbc=jdbc;this.resolver=resolver;}
  @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String tenantKey = extract(req);
    var tenant = resolver.resolve(tenantKey);
    if (tenant == null) { res.sendError(404, "Tenant not found or inactive"); return; }
    String tid = tenant.id().toString();
    TenantContext.set(tid); MDC.put("tenant_id", tid);
    try { jdbc.execute("select set_config('app.current_tenant', '" + tid + "', true)"); chain.doFilter(req, res); }
    finally { TenantContext.clear(); MDC.remove("tenant_id"); }
  }
  private String extract(HttpServletRequest req) {
    String host = req.getHeader("host");
    if (host != null) { Matcher m = SUBDOMAIN.matcher(host); if (m.find()) return m.group(1); }
    String hdr = req.getHeader("X-Tenant-ID"); if (hdr != null && !hdr.isBlank()) return hdr;
    String claim = req.getHeader("X-Auth-Tenant"); if (claim != null && !claim.isBlank()) return claim;
    throw new IllegalArgumentException("Tenant not resolved");
  }
}
