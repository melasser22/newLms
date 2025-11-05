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
        Map.of("shared.security.hs256.secret", "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="));
    SharedSecurityProps props = new Binder(source)
        .bind("shared.security", SharedSecurityProps.class).get();
    assertEquals("MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=", props.getHs256().getSecret());
  }

  @Test
  void bindsAllowedOrigins() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource(
        Map.of("shared.security.resource-server.allowed-origins[0]", "https://app.example.com"));
    SharedSecurityProps props = new Binder(source)
        .bind("shared.security", SharedSecurityProps.class).get();
    assertEquals(List.of("https://app.example.com"), props.getResourceServer().getAllowedOrigins());
  }
}
