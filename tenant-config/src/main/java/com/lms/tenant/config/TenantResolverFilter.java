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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract tenant key (subdomain or header), resolve to UUID, push to TenantContext + MDC,
 * and set Postgres GUC `app.current_tenant` for the current connection.
 */
@Component
public class TenantResolverFilter extends OncePerRequestFilter {
  private static final Pattern SUBDOMAIN = Pattern.compile("^([a-z0-9-]+)\\.");
  private final JdbcTemplate jdbc;
  private final TenantResolverService resolver;
  private final TenantResolutionProperties props;

  public TenantResolverFilter(JdbcTemplate jdbc, TenantResolverService resolver, TenantResolutionProperties props) {
    this.jdbc = jdbc; this.resolver = resolver; this.props = props;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String tenantKey = extract(req);
    var tenant = resolver.resolve(tenantKey);
    if (tenant == null) { res.sendError(404, "Tenant not found or inactive"); return; }

    String tid = tenant.id().toString();
    TenantContext.set(tid);
    MDC.put("tenant_id", tid);
    try {
      // Set once for the current connection if re-used by JdbcTemplate; wrapped DataSource will handle others
      jdbc.execute("select set_config('app.current_tenant', '" + tid + "', true)");
      chain.doFilter(req, res);
    } finally {
      TenantContext.clear();
      MDC.remove("tenant_id");
    }
  }

  private String extract(HttpServletRequest req) {
    if (props.isUseSubdomain()) {
      String host = req.getHeader("host");
      if (host != null) {
        Matcher m = SUBDOMAIN.matcher(host);
        if (m.find()) return m.group(1);
      }
    }
    String hdr = req.getHeader(props.getHeaderPrimary());
    if (hdr != null && !hdr.isBlank()) return hdr;
    String claim = req.getHeader(props.getHeaderSecondary());
    if (claim != null && !claim.isBlank()) return claim;
    throw new IllegalArgumentException("Tenant not resolved");
  }
}
