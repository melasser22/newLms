package com.ejada.gateway.routes.service;

import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.routes.model.RouteDefinition;
import com.ejada.gateway.routes.model.RouteMetadata;
import com.ejada.gateway.routes.model.RouteMetadata.TenantRoutingRule;
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
    validateTenantRouting(definition.metadata());
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
    boolean canaryPresent = false;
    for (TrafficSplit split : metadata.getTrafficSplits()) {
      if (split.getPercentage() <= 0) {
        throw new RouteValidationException("Traffic split percentages must be positive");
      }
      if (!StringUtils.hasText(split.getVariantId())) {
        throw new RouteValidationException("Traffic split variantId must be provided");
      }
      if ("canary".equalsIgnoreCase(split.getVariantId())) {
        canaryPresent = true;
        if (split.getPercentage() != 10) {
          throw new RouteValidationException("Canary routes must reserve exactly 10% of traffic");
        }
      }
      if (split.getServiceUri() == null && metadata.resolveEffectiveUri(null).isEmpty()) {
        throw new RouteValidationException("Traffic split requires service URI override when base URI missing");
      }
      total += split.getPercentage();
    }
    if (total != 100) {
      throw new RouteValidationException("Traffic split percentages must total 100 but were " + total);
    }
    if (canaryPresent && metadata.getTrafficSplits().size() < 2) {
      throw new RouteValidationException("Canary deployments require at least one stable variant alongside the canary");
    }
  }

  private void validateTenantRouting(RouteMetadata metadata) {
    if (metadata == null || metadata.getTenantRouting().isEmpty()) {
      return;
    }
    for (var entry : metadata.getTenantRouting().entrySet()) {
      String tenantId = entry.getKey();
      TenantRoutingRule rule = entry.getValue();
      if (!StringUtils.hasText(tenantId)) {
        throw new RouteValidationException("Tenant routing entries must specify a tenant id");
      }
      if (rule == null) {
        continue;
      }
      for (String instance : rule.getDedicatedInstances()) {
        if (!StringUtils.hasText(instance)) {
          throw new RouteValidationException("Tenant routing dedicated instances must not contain blanks");
        }
      }
      for (var weightEntry : rule.getInstanceWeights().entrySet()) {
        if (!StringUtils.hasText(weightEntry.getKey())) {
          throw new RouteValidationException("Tenant routing weight entries must specify instance ids");
        }
        if (weightEntry.getValue() == null || weightEntry.getValue() <= 0) {
          throw new RouteValidationException("Tenant routing weight for instance "
              + weightEntry.getKey() + " must be positive");
        }
      }
    }
  }

  public static class RouteValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RouteValidationException(String message) {
      super(message);
    }
  }
}
