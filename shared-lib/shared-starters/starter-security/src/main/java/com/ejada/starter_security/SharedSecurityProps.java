package com.ejada.starter_security;

import com.ejada.common.BaseStarterProperties;
import com.ejada.common.constants.HeaderNames;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Security starter properties.
 *
 * Root-level JWT settings kept for backward compatibility:
 *   shared.security.mode: hs256 | jwks | issuer
 *   shared.security.hs256.secret
 *   shared.security.jwks.uri
 *   shared.security.issuer
 *   shared.security.audience
 *   shared.security.roles-claim
 *   shared.security.tenant-claim
 *
 * New Resource Server block:
 *   shared.security.resource-server.enabled
 *   shared.security.resource-server.permit-all[...]
 *   shared.security.resource-server.disable-csrf
 *   shared.security.resource-server.stateless
 *   shared.security.resource-server.verify-tenant-claim
 *
 * Tenant verification block:
 *   shared.security.tenant-verification.strict-mode
 *   shared.security.tenant-verification.require-tenant-header
 */
@Getter
@Validated
@ConfigurationProperties(prefix = "shared.security")
public class SharedSecurityProps implements BaseStarterProperties {

  // --------- JWT (back-compat root) ---------
  /** hs256 | jwks | issuer */
  @Setter private String mode = "hs256";

  private Hs256 hs256 = new Hs256();
  private Jwks jwks = new Jwks();

  /**
   * Legacy block kept for backward compatibility with the previous
   * {@code shared.security.jwt.*} properties.
   */
  private LegacyJwt jwt = new LegacyJwt();

  @Setter private String issuer;                // when mode = issuer
  @Setter private String audience;              // optional audience check

  /** Claim that carries roles (e.g., "roles", "realm_access.roles", "authorities"). */
  @Setter private String rolesClaim = "roles";

  /** Additional standard scope claim (space-delimited) if you map scopes to authorities. */
  @Setter private String scopeClaim = "scope";

  /** Claim that carries the tenant id (if you wish to resolve tenant from JWT). */
  @Setter private String tenantClaim = "tenant";

  /** Prefix for scope authorities mapped from scope/scopeClaim. */
  @Setter private String authorityPrefix = "SCOPE_";

  /** Prefix for role authorities mapped from rolesClaim. */
  @Setter private String rolePrefix = "ROLE_";

  /** Master switch to enable or disable role checks in services. */
  @Setter private boolean enableRoleCheck = true;

  // --------- Resource Server defaults ---------
  private ResourceServer resourceServer = new ResourceServer();
  private InternalClient internalClient = new InternalClient();

  /** Tenant context verification defaults. */
  private TenantVerification tenantVerification = new TenantVerification();

  // ===========================================
  //            Nested types
  // ===========================================
  @Getter
  @Setter
  public static class Hs256 {
    /** Shared secret for HMAC signing. Must be provided via configuration. */
    @jakarta.validation.constraints.NotBlank
    private String secret;
  }

  @Getter
  @Setter
  public static class Jwks {
    /** JWK Set URI when mode = jwks */
    private String uri;
  }

  @Getter
  @Setter
  public static class ResourceServer {
    /** Master enable switch for the SecurityFilterChain */
    private boolean enabled = true;

    /** Enforce tenant claim validation against inbound headers. */
    private boolean verifyTenantClaim = false;

    /** Permit-all endpoints (ant matchers). */
    private String[] permitAll = new String[]{
        "/actuator/health",
        "/v3/api-docs/**",
        "/**/v3/api-docs/**",
        "/swagger-ui/**",
        "/**/swagger-ui/**",
        // common public endpoints (with and without /api version prefix):
        "/auth/**", "/api/*/auth/**",
        "/login", "/register",
        "/config/**"
    };

    /** Disable CSRF for stateless REST APIs. */
    private boolean disableCsrf = true;

    /** Patterns that should bypass CSRF checks when CSRF is enabled. */
    private String[] csrfIgnore = new String[]{
        "/auth/**", "/api/*/auth/**",
        "/login", "/register"
    };

    /** Force stateless sessions for APIs. */
    private boolean stateless = true;

    /** Allowed CORS origins. */
    private List<String> allowedOrigins = List.of();
  }

  @Getter
  @Setter
  public static class InternalClient {
    /** Enable internal service authentication via shared secret. */
    private boolean enabled = false;

    /** Header carrying the internal authentication secret. */
    private String headerName = HeaderNames.INTERNAL_AUTH;

    /** Shared API key expected from internal callers. */
    private String apiKey;

    /** Logical principal applied to the authenticated request. */
    private String principal = "internal-service";
  }

  @Getter
  @Setter
  public static class LegacyJwt {
    /** Legacy secret property from shared.security.jwt.secret. */
    private String secret;

    /** Legacy token-period property retained for downstream usage. */
    private String tokenPeriod;
  }

  @Getter
  @Setter
  public static class TenantVerification {
    /**
     * Strict mode enforces tenant equality between JWT claims, headers and the
     * current tenant context.
     */
    private boolean strictMode = false;

    /** Require presence of X-Tenant-Id header on authenticated requests. */
    private boolean requireTenantHeader = false;

    /** Enable request path/body validation to prevent cross-tenant access. */
    private boolean preventCrossTenantAccess = false;

    /** JSON field names inspected when validating request bodies. */
    private String[] bodyTenantFields = new String[]{"tenantId", "tenant_id"};
  }

  public void setHs256(Hs256 hs256) {
    this.hs256 = hs256 != null ? hs256 : new Hs256();
    applyLegacySecretFallback();
  }

  public void setJwks(Jwks jwks) {
    this.jwks = jwks != null ? jwks : new Jwks();
  }

  public void setResourceServer(ResourceServer resourceServer) {
    this.resourceServer = resourceServer != null ? resourceServer : new ResourceServer();
  }

  public void setInternalClient(InternalClient internalClient) {
    this.internalClient = internalClient != null ? internalClient : new InternalClient();
  }
  public void setTenantVerification(TenantVerification tenantVerification) {
    this.tenantVerification =
        tenantVerification != null ? tenantVerification : new TenantVerification();
  }

  public void setJwt(LegacyJwt jwt) {
    this.jwt = jwt != null ? jwt : new LegacyJwt();
    applyLegacySecretFallback();
  }

  private void applyLegacySecretFallback() {
    if (this.hs256 == null) {
      this.hs256 = new Hs256();
    }
    String hsSecret = this.hs256.getSecret();
    String legacySecret = this.jwt != null ? this.jwt.getSecret() : null;
    if ((hsSecret == null || hsSecret.isBlank()) && legacySecret != null && !legacySecret.isBlank()) {
      this.hs256.setSecret(legacySecret);
    }
  }
}
