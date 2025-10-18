package com.ejada.gateway.admin.model;

import com.ejada.gateway.config.GatewayRoutesProperties;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Lightweight DTO describing a configured route. Exposed via {@code /api/v1/admin/routes} so
 * operators can inspect gateway routing behaviour without logging into the cluster.
 */
public record AdminRouteView(
    String id,
    String targetUri,
    List<String> paths,
    List<String> methods,
    int stripPrefix,
    String prefixPath,
    boolean versioningEnabled,
    String defaultVersion,
    List<String> supportedVersions,
    boolean sessionAffinityEnabled,
    String sessionAffinityCookie,
    String sessionAffinityHeader,
    boolean weightEnabled,
    String weightGroup,
    int weightValue,
    Map<String, String> requestHeaders,
    boolean resilienceEnabled,
    String fallbackUri,
    String circuitBreakerName,
    String loadBalancingStrategy) {

  public AdminRouteView {
    paths = paths == null ? List.of() : List.copyOf(paths);
    methods = methods == null ? List.of() : List.copyOf(methods);
    supportedVersions = supportedVersions == null ? List.of() : List.copyOf(supportedVersions);
    requestHeaders = requestHeaders == null ? Map.of() : Map.copyOf(requestHeaders);
  }

  public static AdminRouteView fromRoute(GatewayRoutesProperties.ServiceRoute route) {
    GatewayRoutesProperties.ServiceRoute.Versioning versioning = route.getVersioning();
    GatewayRoutesProperties.ServiceRoute.SessionAffinity affinity = route.getSessionAffinity();
    GatewayRoutesProperties.ServiceRoute.Weight weight = route.getWeight();
    GatewayRoutesProperties.ServiceRoute.Resilience resilience = route.getResilience();
    URI uri = route.getUri();

    return new AdminRouteView(
        route.getId(),
        uri != null ? uri.toString() : "",
        List.copyOf(route.getPaths()),
        List.copyOf(route.getMethods()),
        route.getStripPrefix(),
        route.getPrefixPath(),
        versioning.isEnabled(),
        versioning.isEnabled() ? versioning.getDefaultVersion() : null,
        versioning.isEnabled() ? List.copyOf(versioning.getSupportedVersions()) : List.of(),
        affinity.isEnabled(),
        affinity.isEnabled() ? affinity.getCookieName() : null,
        affinity.isEnabled() ? affinity.getHeaderName() : null,
        weight.isEnabled(),
        weight.isEnabled() ? weight.getGroup() : null,
        weight.isEnabled() ? weight.getValue() : 0,
        Map.copyOf(route.getRequestHeaders()),
        resilience.isEnabled(),
        resilience.isEnabled() ? resilience.resolvedFallbackUri(route.getId()) : null,
        resilience.isEnabled() ? resilience.resolvedCircuitBreakerName(route.getId()) : null,
        route.getLbStrategy().name());
  }
}
