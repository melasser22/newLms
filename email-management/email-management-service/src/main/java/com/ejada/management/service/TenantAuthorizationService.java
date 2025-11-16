package com.ejada.management.service;

import com.ejada.management.config.TenantSecurityProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TenantAuthorizationService {

  private final TenantSecurityProperties properties;

  public TenantAuthorizationService(TenantSecurityProperties properties) {
    this.properties = new TenantSecurityProperties();
    this.properties.setAuthenticationRequired(properties.isAuthenticationRequired());
    this.properties.setTokens(properties.getTokens());
    this.properties.setRateLimit(properties.getRateLimit());
  }

  public void verifyAccess(String tenantId, String presentedToken) {
    if (!properties.isAuthenticationRequired()) {
      return;
    }
    String expectedToken = properties.getTokens().get(tenantId);
    if (!StringUtils.hasText(expectedToken) || !expectedToken.equals(presentedToken)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid tenant token");
    }
  }
}
