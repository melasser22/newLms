package com.ejada.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ApplicationYamlRoutesSmokeTest {

  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  @Test
  void applicationYamlDefinesCoreServiceRoutes() throws IOException {
    try (InputStream inputStream = new ClassPathResource("application.yaml").getInputStream()) {
      JsonNode root = YAML_MAPPER.readTree(inputStream);
      JsonNode routes = root.path("gateway").path("routes");

      assertThat(routes.isMissingNode() || routes.isNull())
          .as("gateway.routes should be present in application.yaml")
          .isFalse();

      Set<String> routeKeys = new LinkedHashSet<>();
      Iterator<String> fieldNames = routes.fieldNames();
      fieldNames.forEachRemaining(routeKeys::add);

      assertThat(routeKeys)
          .as("Static routes declared in application.yaml")
          .contains("setup", "tenant", "tenant-canary", "catalog", "subscription", "billing", "policy");

      assertThat(routes.path("tenant").path("paths").isArray())
          .as("tenant route paths should be defined")
          .isTrue();
      assertThat(routes.path("catalog").path("paths").isArray())
          .as("catalog route paths should be defined")
          .isTrue();

      JsonNode misplacedRoutes = root.path("jasypt").path("encryptor").path("property");
      assertThat(misplacedRoutes.has("tenant"))
          .as("Routes must not be nested under jasypt.encryptor.property")
          .isFalse();
    }
  }
}
