package com.ejada.starter_security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SharedSecurityPropsTest {

  @Test
  void bindsHs256Secret() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource(
        Map.of("shared.security.hs256.secret", "0123456789ABCDEF0123456789ABCDEF"));
    SharedSecurityProps props = new Binder(source)
        .bind("shared.security", SharedSecurityProps.class).get();
    assertEquals("0123456789ABCDEF0123456789ABCDEF", props.getHs256().getSecret());
  }

  @Test
  void bindsAllowedOrigins() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource(
        Map.of("shared.security.resource-server.allowed-origins[0]", "https://app.example.com"));
    SharedSecurityProps props = new Binder(source)
        .bind("shared.security", SharedSecurityProps.class).get();
    assertEquals(List.of("https://app.example.com"), props.getResourceServer().getAllowedOrigins());
  }

  @Test
  void defaultPermitAllSkipsPrivilegedAdminEndpoints() {
    SharedSecurityProps props = new SharedSecurityProps();
    String[] permitAll = props.getResourceServer().getPermitAll();

    assertArrayEquals(new String[]{
        "/actuator/health",
        "/v3/api-docs/**",
        "/api/*/v3/api-docs/**",
        "/api/*/*/v3/api-docs/**",
        "/swagger-ui/**",
        "/api/*/swagger-ui/**",
        "/api/*/*/swagger-ui/**",
        "/swagger-ui.html",
        "/api/*/swagger-ui.html",
        "/api/*/*/swagger-ui.html",
        "/auth/**",
        "/api/auth/**",
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/reset-password",
        "/api/v1/auth/admin/login",
        "/config/**"
    }, permitAll);
  }
}
