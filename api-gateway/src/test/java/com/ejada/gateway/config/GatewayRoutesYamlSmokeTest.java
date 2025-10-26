package com.ejada.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class GatewayRoutesYamlSmokeTest {

  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  @Test
  void routesYamlDefinesCoreServiceRoutes() throws IOException {
    try (InputStream inputStream = new ClassPathResource("routes.yml").getInputStream()) {
      JsonNode root = YAML_MAPPER.readTree(inputStream);

      JsonNode versioning = root.path("gateway").path("versioning");
      assertThat(versioning.isMissingNode() || versioning.isNull())
          .as("gateway.versioning should be present in routes.yml")
          .isFalse();

      JsonNode routes = root.path("gateway").path("routes");
      assertThat(routes.isMissingNode() || routes.isNull() || routes.size() == 0)
          .as("Static gateway.routes definitions should be managed via the database")
          .isTrue();
    }
  }
}
