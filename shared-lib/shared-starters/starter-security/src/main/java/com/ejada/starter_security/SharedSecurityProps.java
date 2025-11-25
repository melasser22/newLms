package com.ejada.starter_security;


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
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "shared.security")
public class SharedSecurityProps  {

  // --------- JWT (back-compat root) ---------
  /** hs256 | jwks | issuer */
  private String mode = "hs256";

  private Hs256 hs256 = new Hs256();
  private Jwks jwks = new Jwks();

  private String issuer;                // when mode = issuer
  private String audience;              // optional audience check

  /** Claim that carries roles (e.g., "roles", "realm_access.roles", "authorities"). */
  private String rolesClaim = "roles";

  /** Additional standard scope claim (space-delimited) if you map scopes to authorities. */
  private String scopeClaim = "scope";

  /** Claim that carries the tenant id (if you wish to resolve tenant from JWT). */
  private String tenantClaim = "tenant";

  /** Prefix for scope authorities mapped from scope/scopeClaim. */
  private String authorityPrefix = "SCOPE_";

  /** Prefix for role authorities mapped from rolesClaim. */
  private String rolePrefix = "ROLE_";

  /** Master switch to enable or disable role checks in services. */
  private boolean enableRoleCheck = true;

  // --------- Resource Server defaults ---------
  private ResourceServer resourceServer = new ResourceServer();

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

    /** Permit-all endpoints (ant matchers). */
    private String[] permitAll = new String[]{
        "/actuator/health",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        // common public endpoints:
        "/auth/**", "/login", "/register","/config/**"
    };

    /** Disable CSRF for stateless REST APIs. */
    private boolean disableCsrf = true;

    /** Force stateless sessions for APIs. */
    private boolean stateless = true;

    /** Allowed CORS origins. */
    private List<String> allowedOrigins = List.of();
  }
}
