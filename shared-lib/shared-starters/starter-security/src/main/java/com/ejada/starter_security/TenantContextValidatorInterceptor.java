package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UrlPathHelper;

/**
 * Validates tenant context against inbound headers to guard against tampering.
 */
class TenantContextValidatorInterceptor implements HandlerInterceptor {

  private static final Logger log =
      LoggerFactory.getLogger(TenantContextValidatorInterceptor.class);

  private final SharedSecurityProps props;
  private final PathMatcher pathMatcher = new AntPathMatcher();
  private final UrlPathHelper pathHelper = new UrlPathHelper();

  TenantContextValidatorInterceptor(SharedSecurityProps props) {
    this.props = props;
    this.pathHelper.setRemoveSemicolonContent(true);
    this.pathHelper.setUrlDecode(false);
  }

  @Override
  public boolean preHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler) throws Exception {
    SharedSecurityProps.ResourceServer resourceServer = props.getResourceServer();
    SharedSecurityProps.TenantVerification verification = props.getTenantVerification();

    if (resourceServer == null || verification == null) {
      return true;
    }

    if (!resourceServer.isEnabled()) {
      return true;
    }

    if (!resourceServer.isVerifyTenantClaim()
        && !verification.isStrictMode()
        && !verification.isRequireTenantHeader()) {
      return true;
    }

    String headerTenant = normalize(request.getHeader(HeaderNames.X_TENANT_ID));
    String contextTenant = normalize(ContextManager.Tenant.get());

    boolean headerOptional = isTenantHeaderOptional(request, verification);

    if (verification.isRequireTenantHeader() && !headerOptional && headerTenant == null) {
      logMismatch("Missing required tenant header", headerTenant, contextTenant);
      writeViolation(response, HttpServletResponse.SC_BAD_REQUEST,
          "TENANT_HEADER_REQUIRED", "X-Tenant-Id header is required");
      return false;
    }

    if (!headerOptional
        && headerTenant != null
        && contextTenant != null
        && !Objects.equals(headerTenant, contextTenant)) {
      logMismatch("Tenant context mismatch detected", headerTenant, contextTenant);
      if (verification.isStrictMode()) {
        writeViolation(response, HttpServletResponse.SC_BAD_REQUEST,
            "TENANT_CONTEXT_MISMATCH", "Tenant header and context mismatch");
        return false;
      }
    }

    return true;
  }

  private boolean isTenantHeaderOptional(HttpServletRequest request,
                                         SharedSecurityProps.TenantVerification verification) {
    String[] patterns = verification.getHeaderOptionalPatterns();
    if (patterns == null || patterns.length == 0) {
      return false;
    }
    String requestPath = pathHelper.getPathWithinApplication(request);
    for (String pattern : patterns) {
      if (!StringUtils.hasText(pattern)) {
        continue;
      }
      if (pathMatcher.match(pattern.trim(), requestPath)) {
        return true;
      }
    }
    return false;
  }

  private static void writeViolation(HttpServletResponse response,
                                     int status,
                                     String code,
                                     String message) throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter()
        .write(String.format("{\"code\":\"%s\",\"message\":\"%s\"}", code, message));
  }

  private static void logMismatch(String message,
                                  @Nullable String headerTenant,
                                  @Nullable String contextTenant) {
    if (log.isWarnEnabled()) {
      log.warn("{} (header='{}', context='{}')", message, headerTenant, contextTenant);
    }
  }

  private static String normalize(@Nullable String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
