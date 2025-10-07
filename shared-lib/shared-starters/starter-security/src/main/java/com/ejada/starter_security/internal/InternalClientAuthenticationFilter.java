package com.ejada.starter_security.internal;

import com.ejada.common.constants.HeaderNames;
import com.ejada.starter_security.SharedSecurityProps;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates shared secret authentication for intra-service calls bypassing the gateway.
 */
public class InternalClientAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(InternalClientAuthenticationFilter.class);

  private final SharedSecurityProps.InternalClient properties;
  private final String headerName;
  private final byte[] expected;

  public InternalClientAuthenticationFilter(SharedSecurityProps.InternalClient properties) {
    Assert.notNull(properties, "properties must not be null");
    this.properties = properties;
    Assert.isTrue(properties.isEnabled(), "Internal client authentication must be enabled");
    Assert.hasText(properties.getApiKey(), "shared.security.internal-client.api-key must be configured");
    this.headerName = StringUtils.hasText(properties.getHeaderName())
        ? properties.getHeaderName()
        : HeaderNames.INTERNAL_AUTH;
    this.expected = properties.getApiKey().getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!properties.isEnabled()) {
      return true;
    }
    String presented = extractPresentedKey(request);
    return presented == null;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String presented = Objects.requireNonNull(extractPresentedKey(request), "presented secret");
    if (!matches(presented)) {
      log.warn("Rejected internal request with invalid credential from {}", request.getRemoteAddr());
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal service credential");
      return;
    }

    Authentication previous = SecurityContextHolder.getContext().getAuthentication();
    InternalServiceAuthenticationToken token = new InternalServiceAuthenticationToken(
        resolvePrincipal(request), headerName);
    SecurityContextHolder.getContext().setAuthentication(token);
    try {
      filterChain.doFilter(request, response);
    } finally {
      Authentication current = SecurityContextHolder.getContext().getAuthentication();
      if (current == token || current == null) {
        SecurityContextHolder.getContext().setAuthentication(previous);
      }
    }
  }

  private String extractPresentedKey(HttpServletRequest request) {
    String header = trimToNull(request.getHeader(headerName));
    if (!StringUtils.hasText(header)) {
      return null;
    }
    return header;
  }

  private boolean matches(String presented) {
    byte[] candidate = presented.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expected, candidate);
  }

  private String resolvePrincipal(HttpServletRequest request) {
    String origin = trimToNull(request.getHeader(HeaderNames.GATEWAY_ORIGIN));
    if (StringUtils.hasText(origin)) {
      return origin;
    }
    return properties.getPrincipal();
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
