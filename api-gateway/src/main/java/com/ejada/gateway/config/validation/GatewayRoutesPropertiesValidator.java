package com.ejada.gateway.config.validation;

import com.ejada.config.validation.ConfigurationPropertiesValidator;
import com.ejada.config.validation.ConfigurationValidator;
import com.ejada.gateway.config.GatewayRoutesProperties;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Validates declarative route configuration before the gateway finishes
 * starting up. This prevents invalid downstream URIs or path matchers from
 * being registered.
 */
@ConfigurationPropertiesValidator
public class GatewayRoutesPropertiesValidator extends ConfigurationValidator<GatewayRoutesProperties> {

  private static final Set<String> SUPPORTED_SCHEMES = Set.of("http", "https", "lb", "ws", "wss");
  private static final PathPatternParser PATH_PATTERN_PARSER = new PathPatternParser();

  public GatewayRoutesPropertiesValidator() {
    super(GatewayRoutesProperties.class);
  }

  @Override
  protected void doValidate(GatewayRoutesProperties properties, Errors errors) {
    for (Map.Entry<String, GatewayRoutesProperties.ServiceRoute> entry : properties.getRoutes().entrySet()) {
      String key = entry.getKey();
      GatewayRoutesProperties.ServiceRoute route = entry.getValue();
      if (route == null) {
        rejectValue(errors, "routes[" + key + "]", "gateway.routes.null", "Route %s must not be null", key);
        continue;
      }
      try {
        route.validate(key);
      } catch (IllegalStateException ex) {
        reject(errors, "gateway.routes.invalid", ex.getMessage());
        continue;
      }
      validateUri(errors, key, route.getUri());
      validatePaths(errors, key, route.getPaths());
    }
  }

  private void validateUri(Errors errors, String key, URI uri) {
    if (uri == null) {
      return;
    }
    if (!StringUtils.hasText(uri.getScheme())) {
      rejectValue(errors, "routes[" + key + "].uri", "gateway.routes.uri.scheme", "Route %s must declare a scheme", key);
      return;
    }
    String scheme = uri.getScheme().toLowerCase();
    if (!SUPPORTED_SCHEMES.contains(scheme)) {
      rejectValue(errors, "routes[" + key + "].uri", "gateway.routes.uri.unsupported",
          "Route %s URI scheme '%s' is not supported", key, scheme);
    }
    if (uri.getHost() == null && !"lb".equals(scheme)) {
      rejectValue(errors, "routes[" + key + "].uri", "gateway.routes.uri.host",
          "Route %s URI must include a host when not using lb://", key);
    }
  }

  private void validatePaths(Errors errors, String key, Iterable<String> paths) {
    int index = 0;
    for (String path : paths) {
      if (!StringUtils.hasText(path)) {
        rejectValue(errors, "routes[" + key + "].paths[" + index + "]", "gateway.routes.path.blank",
            "Route %s contains a blank path entry", key);
        index++;
        continue;
      }
      try {
        PATH_PATTERN_PARSER.parse(path.trim());
      } catch (IllegalArgumentException ex) {
        rejectValue(errors, "routes[" + key + "].paths[" + index + "]", "gateway.routes.path.invalid",
            "Route %s path '%s' is not a valid Spring path pattern", key, path);
      }
      index++;
    }
  }
}
