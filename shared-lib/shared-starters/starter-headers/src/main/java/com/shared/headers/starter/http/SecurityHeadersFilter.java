
package com.shared.headers.starter.http;

import com.shared.headers.starter.config.SharedHeadersProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class SecurityHeadersFilter implements Filter {

  private final SharedHeadersProperties props;

  public SecurityHeadersFilter(SharedHeadersProperties props) { this.props = props; }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    if (props.getSecurity().isEnabled() && !isExcluded(req.getRequestURI())) {
      // HSTS
      if (props.getSecurity().getHsts().isEnabled()) {
        StringBuilder v = new StringBuilder("max-age=").append(props.getSecurity().getHsts().getMaxAge());
        if (props.getSecurity().getHsts().isIncludeSubdomains()) v.append("; includeSubDomains");
        if (props.getSecurity().getHsts().isPreload()) v.append("; preload");
        res.setHeader("Strict-Transport-Security", v.toString());
      }
      // X-Frame-Options
      if (props.getSecurity().getFrameOptions() != null && !props.getSecurity().getFrameOptions().isBlank()) {
        res.setHeader("X-Frame-Options", props.getSecurity().getFrameOptions());
      }
      // X-Content-Type-Options
      if (props.getSecurity().getContentTypeOptions() != null) {
        res.setHeader("X-Content-Type-Options", props.getSecurity().getContentTypeOptions());
      }
      // Referrer-Policy
      if (props.getSecurity().getReferrerPolicy() != null) {
        res.setHeader("Referrer-Policy", props.getSecurity().getReferrerPolicy());
      }
      // Permissions-Policy
      if (props.getSecurity().getPermissionsPolicy() != null && !props.getSecurity().getPermissionsPolicy().isBlank()) {
        res.setHeader("Permissions-Policy", props.getSecurity().getPermissionsPolicy());
      }
      // Content-Security-Policy
      if (props.getSecurity().getCsp() != null && !props.getSecurity().getCsp().isBlank()) {
        res.setHeader("Content-Security-Policy", props.getSecurity().getCsp());
      }
      // COOP/COEP
      if (props.getSecurity().getCoop() != null && !props.getSecurity().getCoop().isBlank()) {
        res.setHeader("Cross-Origin-Opener-Policy", props.getSecurity().getCoop());
      }
      if (props.getSecurity().getCoep() != null && !props.getSecurity().getCoep().isBlank()) {
        res.setHeader("Cross-Origin-Embedder-Policy", props.getSecurity().getCoep());
      }
      // Legacy extras
      if (props.getSecurity().getxDownloadOptions() != null && !props.getSecurity().getxDownloadOptions().isBlank()) {
        res.setHeader("X-Download-Options", props.getSecurity().getxDownloadOptions());
      }
      if (props.getSecurity().getXssProtection() != null) {
        res.setHeader("X-XSS-Protection", props.getSecurity().getXssProtection());
      }
    }
    chain.doFilter(request, response);
  }

  private boolean isExcluded(String path) {
    for (String p : props.getSecurity().getExcludePaths()) {
      if (p == null || p.isBlank()) continue;
      // very simple matcher: prefix or exact
      if (p.endsWith("/**")) {
        String prefix = p.substring(0, p.length() - 3);
        if (path.startsWith(prefix)) return true;
      } else if (p.equals(path)) {
        return true;
      }
    }
    return false;
  }
}
