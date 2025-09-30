package com.ejada.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class GatewayRoutesPropertiesTest {

  @Test
  void setMethodsNormalisesAndDeduplicatesValues() {
    GatewayRoutesProperties.ServiceRoute route = new GatewayRoutesProperties.ServiceRoute();
    route.setMethods(List.of(" get ", "POST", "get", ""));

    assertThat(route.getMethods()).containsExactly("GET", "POST");
  }

  @Test
  void validateThrowsWhenMethodInvalid() {
    GatewayRoutesProperties.ServiceRoute route = new GatewayRoutesProperties.ServiceRoute();
    route.setId("test");
    route.setUri(URI.create("http://example.com"));
    route.setPaths(List.of("/foo"));
    route.setMethods(List.of("GET", "INVALID"));

    assertThatThrownBy(() -> route.validate("sample"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("INVALID");
  }
}
