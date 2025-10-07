package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
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
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Validates JWTs against the current tenant context and Redis revocation list.
 */
@Component
public class TenantAwareJwtValidator implements OAuth2TokenValidator<Jwt> {

  private static final OAuth2Error TENANT_MISMATCH_ERROR =
      new OAuth2Error("invalid_token", "Token tenant mismatch", null);
  private static final OAuth2Error TOKEN_REVOKED_ERROR =
      new OAuth2Error("token_revoked", "Token has been revoked", null);

  private final String tenantClaim;
  private final StringRedisTemplate redisTemplate;

  public TenantAwareJwtValidator(SharedSecurityProps props,
                                 @Nullable StringRedisTemplate redisTemplate) {
    this.tenantClaim = StringUtils.hasText(props.getTenantClaim())
        ? props.getTenantClaim()
        : "tenant_id";
    this.redisTemplate = redisTemplate;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    String claimValue = normalize(jwt.getClaimAsString(tenantClaim));
    String requestTenant = normalize(getCurrentRequestTenant());

    if (!Objects.equals(claimValue, requestTenant)) {
      return OAuth2TokenValidatorResult.failure(TENANT_MISMATCH_ERROR);
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
      String headerTenant = normalize(request.getHeader(HeaderNames.X_TENANT_ID));
      if (headerTenant != null) {
        return headerTenant;
      }
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
}
