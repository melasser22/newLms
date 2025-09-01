package com.shared.starter_security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Verifies that only known roles are mapped to authorities.
 */
class JwtRoleFilterTest {

  @Test
  void ignoresUnknownRoles() {
    SharedSecurityProps props = new SharedSecurityProps();
    SecurityAutoConfiguration cfg = new SecurityAutoConfiguration();
    JwtAuthenticationConverter conv = cfg.jwtAuthenticationConverter(props);

    Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
        .claim("roles", List.of("TENANT_ADMIN", "FAKE_ROLE"))
        .build();

    var auth = conv.convert(jwt);
    assertNotNull(auth);
    assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN")));
    assertFalse(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_FAKE_ROLE")));
  }
}

