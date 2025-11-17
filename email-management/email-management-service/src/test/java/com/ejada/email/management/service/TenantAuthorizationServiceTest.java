package com.ejada.email.management.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ejada.email.management.config.TenantSecurityProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class TenantAuthorizationServiceTest {

  @Test
  void shouldAllowAccessWhenAuthenticationDisabled() {
    TenantSecurityProperties properties = new TenantSecurityProperties();
    properties.setAuthenticationRequired(false);

    TenantAuthorizationService service = new TenantAuthorizationService(properties);

    assertThatCode(() -> service.verifyAccess("tenant-1", null)).doesNotThrowAnyException();
  }

  @Test
  void shouldThrowWhenTokenMissingOrInvalid() {
    TenantSecurityProperties properties = new TenantSecurityProperties();
    properties.setAuthenticationRequired(true);
    properties.setTokens(Map.of("tenant-2", "expected-token"));

    TenantAuthorizationService service = new TenantAuthorizationService(properties);

    assertThatThrownBy(() -> service.verifyAccess("tenant-2", "wrong-token"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Invalid tenant token");
  }

  @Test
  void shouldAllowWhenTokenMatches() {
    TenantSecurityProperties properties = new TenantSecurityProperties();
    properties.setAuthenticationRequired(true);
    properties.setTokens(Map.of("tenant-3", "expected-token"));

    TenantAuthorizationService service = new TenantAuthorizationService(properties);

    assertThatCode(() -> service.verifyAccess("tenant-3", "expected-token"))
        .doesNotThrowAnyException();
  }
}
