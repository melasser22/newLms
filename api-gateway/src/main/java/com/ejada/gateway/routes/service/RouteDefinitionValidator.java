package com.ejada.gateway.routes.service;

import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.routes.model.RouteDefinition;
import com.ejada.gateway.routes.model.RouteMetadata;
import com.ejada.gateway.routes.model.RouteMetadata.TrafficSplit;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class RouteDefinitionValidator {

  private final RouteDefinitionConverter converter;

  public RouteDefinitionValidator(RouteDefinitionConverter converter) {
    this.converter = converter;
  }

  public RouteDefinition validate(RouteDefinition definition) {
    if (definition == null) {
      throw new RouteValidationException("Route definition must not be null");
    }
    if (!StringUtils.hasText(definition.pathPattern())) {
      throw new RouteValidationException("Path pattern is required");
    }
    if (definition.serviceUri() == null && definition.metadata().resolveEffectiveUri(null).isEmpty()) {
      throw new RouteValidationException("Service URI or active blue/green URI is required");
    }
    validateTrafficSplits(definition.metadata());
    List<GatewayRoutesProperties.ServiceRoute> serviceRoutes = converter.toServiceRoutes(definition);
    for (GatewayRoutesProperties.ServiceRoute serviceRoute : serviceRoutes) {
      serviceRoute.applyDefaults(new GatewayRoutesProperties.RouteDefaults());
      serviceRoute.validate(serviceRoute.getId());
    }
    return definition;
  }

  private void validateTrafficSplits(RouteMetadata metadata) {
    if (metadata == null || CollectionUtils.isEmpty(metadata.getTrafficSplits())) {
      return;
    }
    int total = 0;
    for (TrafficSplit split : metadata.getTrafficSplits()) {
      if (split.getPercentage() <= 0) {
        throw new RouteValidationException("Traffic split percentages must be positive");
      }
      if (split.getServiceUri() == null && metadata.resolveEffectiveUri(null).isEmpty()) {
        throw new RouteValidationException("Traffic split requires service URI override when base URI missing");
      }
      total += split.getPercentage();
    }
    if (total != 100) {
      throw new RouteValidationException("Traffic split percentages must total 100 but were " + total);
    }
  }

  public static class RouteValidationException extends RuntimeException {

    public RouteValidationException(String message) {
      super(message);
    }
  }
}
