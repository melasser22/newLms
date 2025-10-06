package com.ejada.gateway.routes.service;

import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.routes.model.RouteComponent;
import com.ejada.gateway.routes.model.RouteDefinition;
import com.ejada.gateway.routes.model.RouteMetadata;
import com.ejada.gateway.routes.model.RouteMetadata.TrafficSplit;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RouteDefinitionConverter {

  public List<GatewayRoutesProperties.ServiceRoute> toServiceRoutes(RouteDefinition definition) {
    RouteDefinition resolved = definition.ensureServiceUri();
    URI baseUri = resolved.metadata().resolveEffectiveUri(resolved.serviceUri()).orElse(resolved.serviceUri());
    List<GatewayRoutesProperties.ServiceRoute> routes = new ArrayList<>();
    if (resolved.hasTrafficSplits()) {
      String group = resolved.id().toString();
      for (TrafficSplit split : resolved.metadata().getTrafficSplits()) {
        GatewayRoutesProperties.ServiceRoute route = createBaseRoute(resolved, split.getServiceUri() != null
            ? split.getServiceUri()
            : baseUri);
        route.setId(group + "-" + normaliseVariant(split.getVariantId()));
        route.getWeight().setEnabled(true);
        route.getWeight().setGroup(group);
        route.getWeight().setValue(split.getPercentage());
        routes.add(route);
      }
    } else {
      GatewayRoutesProperties.ServiceRoute route = createBaseRoute(resolved, baseUri);
      route.setId(resolved.id().toString());
      routes.add(route);
    }
    return routes;
  }

  private GatewayRoutesProperties.ServiceRoute createBaseRoute(RouteDefinition definition, URI uri) {
    GatewayRoutesProperties.ServiceRoute route = new GatewayRoutesProperties.ServiceRoute();
    route.setUri(uri);
    applyPathPatterns(definition, route);
    applyPredicates(definition.predicates(), route);
    applyFilters(definition.filters(), route);
    applyMetadata(definition.metadata(), route);
    return route;
  }

  private void applyPathPatterns(RouteDefinition definition, GatewayRoutesProperties.ServiceRoute route) {
    List<String> patterns = new ArrayList<>();
    if (StringUtils.hasText(definition.pathPattern())) {
      patterns.addAll(splitPatterns(definition.pathPattern()));
    }
    route.setPaths(patterns);
  }

  private void applyMetadata(RouteMetadata metadata, GatewayRoutesProperties.ServiceRoute route) {
    if (metadata == null) {
      return;
    }
    if (!metadata.getMethods().isEmpty()) {
      route.setMethods(metadata.getMethods());
    }
    if (metadata.getStripPrefix() != null) {
      route.setStripPrefix(metadata.getStripPrefix());
    }
    if (StringUtils.hasText(metadata.getPrefixPath())) {
      route.setPrefixPath(metadata.getPrefixPath());
    }
    if (!metadata.getRequestHeaders().isEmpty()) {
      route.setRequestHeaders(metadata.getRequestHeaders());
    }
  }

  private void applyPredicates(List<RouteComponent> predicates, GatewayRoutesProperties.ServiceRoute route) {
    if (predicates == null) {
      return;
    }
    for (RouteComponent predicate : predicates) {
      String name = predicate.name().toLowerCase(Locale.ROOT);
      Map<String, String> args = predicate.args();
      if ("path".equals(name)) {
        List<String> patterns = args.values().stream()
            .filter(StringUtils::hasText)
            .flatMap(value -> splitPatterns(value).stream())
            .collect(Collectors.toCollection(ArrayList::new));
        if (!patterns.isEmpty()) {
          route.getPaths().addAll(patterns);
        }
      } else if ("method".equals(name)) {
        List<String> methods = args.values().stream()
            .filter(StringUtils::hasText)
            .map(value -> value.trim().toUpperCase(Locale.ROOT))
            .toList();
        if (!methods.isEmpty()) {
          List<String> merged = new ArrayList<>(route.getMethods());
          merged.addAll(methods);
          route.setMethods(merged);
        }
      }
    }
  }

  private void applyFilters(List<RouteComponent> filters, GatewayRoutesProperties.ServiceRoute route) {
    if (filters == null) {
      return;
    }
    for (RouteComponent filter : filters) {
      Map<String, String> args = filter.args();
      switch (filter.name().toLowerCase(Locale.ROOT)) {
        case "stripprefix" -> {
          String parts = resolveFirst(args);
          if (StringUtils.hasText(parts)) {
            route.setStripPrefix(Integer.parseInt(parts));
          }
        }
        case "prefixpath" -> {
          String prefix = resolveFirst(args);
          if (StringUtils.hasText(prefix)) {
            route.setPrefixPath(prefix);
          }
        }
        case "addrequestheader" -> {
          String headerName = args.getOrDefault("name", args.get("_genkey_0"));
          String headerValue = args.getOrDefault("value", args.get("_genkey_1"));
          if (StringUtils.hasText(headerName) && StringUtils.hasText(headerValue)) {
            Map<String, String> headers = new LinkedHashMap<>(route.getRequestHeaders());
            headers.put(headerName, headerValue);
            route.setRequestHeaders(headers);
          }
        }
        default -> {
          // unsupported filter types are ignored for runtime registration but retained for UI
        }
      }
    }
  }

  private String resolveFirst(Map<String, String> args) {
    if (args == null || args.isEmpty()) {
      return null;
    }
    return args.values().iterator().next();
  }

  private List<String> splitPatterns(String value) {
    String[] tokens = value.split(",");
    List<String> result = new ArrayList<>();
    for (String token : tokens) {
      if (StringUtils.hasText(token)) {
        String candidate = token.trim();
        if (!isExcluded(candidate)) {
          result.add(candidate);
        }
      }
    }
    return result;
  }

  private boolean isExcluded(String pattern) {
    if (!StringUtils.hasText(pattern)) {
      return true;
    }
    String candidate = pattern.trim();
    if (candidate.startsWith("/actuator")) {
      return true;
    }
    return "/health".equals(candidate) || candidate.startsWith("/health/");
  }

  private String normaliseVariant(String value) {
    if (!StringUtils.hasText(value)) {
      return "variant";
    }
    return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
  }
}
