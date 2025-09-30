package com.ejada.gateway.config;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Programmatic route configuration so we can reuse the shared properties and
 * enforce consistent filters across environments.
 */
@Configuration
@EnableConfigurationProperties(GatewayRoutesProperties.class)
public class GatewayRoutesConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(GatewayRoutesConfiguration.class);

  @Bean
  RouteLocator gatewayRoutes(RouteLocatorBuilder builder, GatewayRoutesProperties properties) {
    RouteLocatorBuilder.Builder routes = builder.routes();

    for (Map.Entry<String, GatewayRoutesProperties.ServiceRoute> entry : properties.getRoutes().entrySet()) {
      GatewayRoutesProperties.ServiceRoute route = entry.getValue();
      route.validate(entry.getKey());

      routes.route(route.getId(), predicate -> predicate
          .path(route.getPaths().stream()
              .filter(StringUtils::hasText)
              .map(String::trim)
              .toArray(String[]::new))
          .filters(filters -> {
            if (route.getStripPrefix() > 0) {
              filters.stripPrefix(route.getStripPrefix());
            }
            return filters;
          })
          .uri(route.getUri()));

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Registered route {} -> {} ({})", route.getId(), route.getUri(), route.getPaths());
      }
    }

    return routes.build();
  }
}
