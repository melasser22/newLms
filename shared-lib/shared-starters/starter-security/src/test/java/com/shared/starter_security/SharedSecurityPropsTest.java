package com.shared.starter_security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SharedSecurityPropsTest {

  @Test
  void bindsHs256Secret() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource(
        Map.of("shared.security.hs256.secret", "s3cr3t"));
    SharedSecurityProps props = new Binder(source)
        .bind("shared.security", SharedSecurityProps.class).get();
    assertEquals("s3cr3t", props.getHs256().getSecret());
  }
}
