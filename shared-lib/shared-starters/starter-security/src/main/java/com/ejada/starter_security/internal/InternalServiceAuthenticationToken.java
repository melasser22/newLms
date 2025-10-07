package com.ejada.starter_security.internal;

import java.util.Collection;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;

/**
 * Authentication token used for trusted internal service calls.
 */
public class InternalServiceAuthenticationToken extends AbstractAuthenticationToken {

  private static final long serialVersionUID = 1L;

  private final String principal;
  private final String headerName;

  public InternalServiceAuthenticationToken(String principal, String headerName) {
    this(principal, headerName, defaultAuthorities());
  }

  public InternalServiceAuthenticationToken(
      String principal,
      String headerName,
      Collection<? extends GrantedAuthority> authorities) {
    super(authorities == null ? defaultAuthorities() : List.copyOf(authorities));
    this.principal = StringUtils.hasText(principal) ? principal : "internal-service";
    this.headerName = headerName;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return "";
  }

  @Override
  public Object getPrincipal() {
    return principal;
  }

  public String getHeaderName() {
    return headerName;
  }

  private static List<GrantedAuthority> defaultAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_SERVICE"));
  }
}
