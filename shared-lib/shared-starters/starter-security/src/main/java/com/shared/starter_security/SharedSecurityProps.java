package com.shared.starter_security;

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
@Validated
@ConfigurationProperties(prefix = "shared.security")
public class SharedSecurityProps {

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

  // --------- Resource Server defaults ---------
  private ResourceServer resourceServer = new ResourceServer();

  // ===========================================
  //            Nested types / getters
  // ===========================================
  public static class Hs256 {
    /** DO NOT use the default in production. Override via config. */
    private String secret = "dev-secret";
    public String getSecret() { return secret; }
    public void setSecret(String s) { this.secret = s; }
  }

  public static class Jwks {
    /** JWK Set URI when mode = jwks */
    private String uri;
    public String getUri() { return uri; }
    public void setUri(String u) { this.uri = u; }
  }

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

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String[] getPermitAll() { return permitAll; }
    public void setPermitAll(String[] permitAll) { this.permitAll = permitAll; }

    public boolean isDisableCsrf() { return disableCsrf; }
    public void setDisableCsrf(boolean disableCsrf) { this.disableCsrf = disableCsrf; }

    public boolean isStateless() { return stateless; }
    public void setStateless(boolean stateless) { this.stateless = stateless; }
  }

  // --------- Getters / Setters ---------
  public String getMode() { return mode; }
  public void setMode(String mode) { this.mode = mode; }

  public Hs256 getHs256() { return hs256; }
  public void setHs256(Hs256 h) { this.hs256 = h; }

  public Jwks getJwks() { return jwks; }
  public void setJwks(Jwks j) { this.jwks = j; }

  public String getIssuer() { return issuer; }
  public void setIssuer(String i) { this.issuer = i; }

  public String getAudience() { return audience; }
  public void setAudience(String a) { this.audience = a; }

  public String getRolesClaim() { return rolesClaim; }
  public void setRolesClaim(String r) { this.rolesClaim = r; }

  public String getScopeClaim() { return scopeClaim; }
  public void setScopeClaim(String scopeClaim) { this.scopeClaim = scopeClaim; }

  public String getTenantClaim() { return tenantClaim; }
  public void setTenantClaim(String t) { this.tenantClaim = t; }

  public String getAuthorityPrefix() { return authorityPrefix; }
  public void setAuthorityPrefix(String authorityPrefix) { this.authorityPrefix = authorityPrefix; }

  public String getRolePrefix() { return rolePrefix; }
  public void setRolePrefix(String rolePrefix) { this.rolePrefix = rolePrefix; }

  public ResourceServer getResourceServer() { return resourceServer; }
  public void setResourceServer(ResourceServer resourceServer) { this.resourceServer = resourceServer; }
}
