package com.ejada.gateway.config.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.gateway.config.GatewayRoutesProperties;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

class GatewayRoutesPropertiesValidatorTest {

  private final GatewayRoutesPropertiesValidator validator = new GatewayRoutesPropertiesValidator();

  @Test
  void rejectsUnsupportedSchemeAndInvalidPath() {
    GatewayRoutesProperties properties = new GatewayRoutesProperties();
    GatewayRoutesProperties.ServiceRoute route = new GatewayRoutesProperties.ServiceRoute();
    route.setId("bad-service");
    route.setUri(URI.create("ftp://legacy-service"));
    route.setPaths(List.of("/api/legacy/**", "invalid[pattern"));
    properties.getRoutes().put("legacy", route);

    Errors errors = new BeanPropertyBindingResult(properties, "gateway");
    validator.validate(properties, errors);

    assertThat(errors.hasErrors()).isTrue();
    assertThat(errors.getAllErrors()).anySatisfy(objectError ->
        assertThat(objectError.getCode()).contains("gateway.routes"));
  }

  @Test
  void acceptsValidRouteConfiguration() {
    GatewayRoutesProperties properties = new GatewayRoutesProperties();
    GatewayRoutesProperties.ServiceRoute route = new GatewayRoutesProperties.ServiceRoute();
    route.setId("tenant-service");
    route.setUri(URI.create("lb://tenant-service"));
    route.setPaths(List.of("/api/tenants/**"));
    properties.getRoutes().put("tenant", route);

    Errors errors = new BeanPropertyBindingResult(properties, "gateway");
    validator.validate(properties, errors);

    assertThat(errors.hasErrors()).isFalse();
  }
}
