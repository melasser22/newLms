package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Validates JWTs against the current tenant context and Redis revocation list.
 */
@Component
public class TenantAwareJwtValidator implements OAuth2TokenValidator<Jwt> {

  private static final Logger log = LoggerFactory.getLogger(TenantAwareJwtValidator.class);
  private static final OAuth2Error TENANT_MISMATCH_ERROR =
      new OAuth2Error("invalid_token", "Token tenant mismatch", null);
  private static final OAuth2Error TENANT_CONTEXT_MISMATCH_ERROR =
      new OAuth2Error("invalid_token", "Tenant context mismatch", null);
  private static final OAuth2Error TENANT_HEADER_MISSING_ERROR =
      new OAuth2Error("tenant_header_missing", "X-Tenant-Id header is required", null);
  private static final OAuth2Error TOKEN_REVOKED_ERROR =
      new OAuth2Error("token_revoked", "Token has been revoked", null);

  private final String tenantClaim;
  private final StringRedisTemplate redisTemplate;
  private final boolean verifyTenantClaim;
  private final SharedSecurityProps.TenantVerification tenantVerification;

  public TenantAwareJwtValidator(SharedSecurityProps props,
                                 @Nullable StringRedisTemplate redisTemplate) {
    this.tenantClaim = StringUtils.hasText(props.getTenantClaim())
        ? props.getTenantClaim()
        : "tenant_id";
    this.redisTemplate = redisTemplate;
    this.verifyTenantClaim = props.getResourceServer().isVerifyTenantClaim();
    SharedSecurityProps.TenantVerification verification = props.getTenantVerification();
    this.tenantVerification =
        verification != null ? verification : new SharedSecurityProps.TenantVerification();
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    String claimValue = normalize(jwt.getClaimAsString(tenantClaim));
    String headerTenant = getTenantHeader();
    String attributeTenant = getCurrentRequestTenant();
    String contextTenant = attributeTenant;

    boolean strictMode = tenantVerification.isStrictMode();
    boolean requireHeader = tenantVerification.isRequireTenantHeader();
    boolean claimPresent = StringUtils.hasText(claimValue);

    if (requireHeader && headerTenant == null) {
      logMismatch("Missing required tenant header", claimValue, null, contextTenant);
      return OAuth2TokenValidatorResult.failure(TENANT_HEADER_MISSING_ERROR);
    }

    if (verifyTenantClaim) {
      if (headerTenant == null && !claimPresent) {
        // no tenant information present; allow super-admin tokens to proceed
      } else {
        if (headerTenant == null) {
          logMismatch("Tenant header missing while verification enabled", claimValue, null, contextTenant);
          return OAuth2TokenValidatorResult.failure(TENANT_HEADER_MISSING_ERROR);
        }
        if (!claimPresent) {
          logMismatch("JWT missing tenant claim while verification enabled", null, headerTenant, contextTenant);
          return OAuth2TokenValidatorResult.failure(TENANT_MISMATCH_ERROR);
        }
      }
    }

    if (headerTenant != null && claimPresent && !Objects.equals(claimValue, headerTenant)) {
      logMismatch("Tenant header and JWT claim mismatch", claimValue, headerTenant, contextTenant);
      if (verifyTenantClaim || strictMode) {
        return OAuth2TokenValidatorResult.failure(TENANT_MISMATCH_ERROR);
      }
    }

    if (headerTenant != null && !claimPresent) {
      logMismatch("Tenant header present but JWT tenant claim missing", null, headerTenant, contextTenant);
      if (verifyTenantClaim || strictMode) {
        return OAuth2TokenValidatorResult.failure(TENANT_MISMATCH_ERROR);
      }
    }

    if (strictMode) {
      String expectedTenant = headerTenant != null ? headerTenant : (claimPresent ? claimValue : null);
      if (expectedTenant != null && contextTenant != null && !Objects.equals(expectedTenant, contextTenant)) {
        logMismatch("Tenant context mismatch", claimValue, headerTenant, contextTenant);
        return OAuth2TokenValidatorResult.failure(TENANT_CONTEXT_MISMATCH_ERROR);
      }
    }

    if (isTokenRevoked(jwt.getId(), claimValue)) {
      return OAuth2TokenValidatorResult.failure(TOKEN_REVOKED_ERROR);
    }

    return OAuth2TokenValidatorResult.success();
  }

  private boolean isTokenRevoked(@Nullable String tokenId, @Nullable String tenant) {
    if (!StringUtils.hasText(tokenId) || redisTemplate == null) {
      return false;
    }

    String key = "security:jwt:revoked:";
    if (StringUtils.hasText(tenant)) {
      key += tenant + ":";
    }
    key += tokenId;

    Boolean exists = redisTemplate.hasKey(key);
    return Boolean.TRUE.equals(exists);
  }

  private static String normalize(@Nullable String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String getCurrentRequestTenant() {
    String fromContext = normalize(ContextManager.Tenant.get());
    if (fromContext != null) {
      return fromContext;
    }

    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servletAttributes) {
      HttpServletRequest request = servletAttributes.getRequest();
      Object attributeTenant = request.getAttribute(HeaderNames.X_TENANT_ID);
      if (attributeTenant instanceof String attrValue) {
        return normalize(attrValue);
      }
      if (attributeTenant != null) {
        return normalize(String.valueOf(attributeTenant));
      }
    }

    return null;
  }

  private String getTenantHeader() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servletAttributes) {
      HttpServletRequest request = servletAttributes.getRequest();
      return normalize(request.getHeader(HeaderNames.X_TENANT_ID));
    }
    return null;
  }

  private void logMismatch(String message,
                           @Nullable String claimTenant,
                           @Nullable String headerTenant,
                           @Nullable String contextTenant) {
    if (log.isWarnEnabled()) {
      log.warn("{} (claim='{}', header='{}', context='{}')",
          message,
          claimTenant,
          headerTenant,
          contextTenant);
    }
  }
}
