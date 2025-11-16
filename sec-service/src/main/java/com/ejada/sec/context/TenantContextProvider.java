package com.ejada.sec.context;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.common.exception.ValidationException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Provides access to tenant identifiers for the current request. The provider first looks at the
 * propagated header/context and falls back to JWT claims when necessary.
 */
@Component
public class TenantContextProvider {

  private static final Logger log = LoggerFactory.getLogger(TenantContextProvider.class);

  private static final String[] TENANT_CLAIMS = {"tenant", "tenant_id", "tid"};
  private static final String[] INTERNAL_TENANT_CLAIMS = {
      "internal_tenant_id", "internalTenantId", "internal_tid", "itid"
  };

  /** Retrieve the current tenant identifier or throw if unavailable. */
  public UUID requireTenantId() {
    return resolveTenantId()
        .orElseThrow(() -> new ValidationException(
            "Missing tenant context",
            "Tenant id was not provided in headers or token"));
  }

  /** Resolve the tenant identifier if available. */
  public Optional<UUID> resolveTenantId() {
    String tenantFromContext = ContextManager.Tenant.get();
    if (StringUtils.hasText(tenantFromContext)) {
      return Optional.of(parseUuid(tenantFromContext,
          "header '%s'".formatted(HeaderNames.X_TENANT_ID)));
    }
    return resolveFromJwt(TENANT_CLAIMS);
  }

  /** Resolve the internal tenant identifier if available. */
  public Optional<UUID> resolveInternalTenantId() {
    return resolveFromJwt(INTERNAL_TENANT_CLAIMS);
  }

  private Optional<UUID> resolveFromJwt(String[] claimNames) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      for (String claim : claimNames) {
        Object value = jwt.getClaims().get(claim);
        if (value != null) {
          return Optional.of(parseUuid(String.valueOf(value), "JWT claim '%s'".formatted(claim)));
        }
      }
    }
    return Optional.empty();
  }

  private UUID parseUuid(String raw, String source) {
    try {
      return UUID.fromString(raw.trim());
    } catch (IllegalArgumentException ex) {
      log.debug("Failed to parse {} as UUID", source, ex);
      throw new ValidationException(
          "Invalid tenant identifier",
          "%s is not a valid UUID".formatted(source));
    }
  }
}
