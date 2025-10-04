package com.ejada.gateway.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Authentication token representing an API key authenticated request.
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

  private final String apiKey;
  private final String tenantId;

  public ApiKeyAuthenticationToken(String apiKey, String tenantId,
      Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.apiKey = apiKey;
    this.tenantId = tenantId;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return apiKey;
  }

  @Override
  public Object getPrincipal() {
    return tenantId;
  }
}
