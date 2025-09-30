package com.ejada.gateway.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

/**
 * Configuration properties describing the downstream services that should be
 * exposed through the gateway. The structure intentionally mirrors the
 * documented plan so operators can declaratively manage routes per
 * environment.
 */
@ConfigurationProperties(prefix = "gateway")
public class GatewayRoutesProperties {

  private final Map<String, ServiceRoute> routes = new LinkedHashMap<>();

  public Map<String, ServiceRoute> getRoutes() {
    return routes;
  }

  /**
   * Route definition for a downstream service.
   */
  public static class ServiceRoute {

    /** Identifier used for RouteLocator registration. */
    private String id;

    /** Downstream service URI (e.g. http://tenant-service:8080). */
    private URI uri;

    /** Paths that should map to the downstream service. */
    private List<String> paths = new ArrayList<>();

    /** Optional HTTP methods to restrict the route (empty = all). */
    private List<String> methods = new ArrayList<>();

    /** Number of path segments to strip before forwarding. */
    private int stripPrefix = 1;

    /** Optional resilience configuration for the route. */
    private Resilience resilience = new Resilience();

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public URI getUri() {
      return uri;
    }

    public void setUri(URI uri) {
      this.uri = uri;
    }

    public void setUri(String value) {
      if (StringUtils.hasText(value)) {
        this.uri = URI.create(value.trim());
      } else {
        this.uri = null;
      }
    }

    public List<String> getPaths() {
      return paths;
    }

    public void setPaths(List<String> paths) {
      this.paths = (paths == null) ? new ArrayList<>() : new ArrayList<>(paths);
    }

    public List<String> getMethods() {
      return methods;
    }

    public void setMethods(List<String> methods) {
      if (methods == null) {
        this.methods = new ArrayList<>();
        return;
      }
      Set<String> normalized = methods.stream()
          .filter(StringUtils::hasText)
          .map(String::trim)
          .map(value -> value.toUpperCase(Locale.ROOT))
          .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
      this.methods = new ArrayList<>(normalized);
    }

    public int getStripPrefix() {
      return stripPrefix;
    }

    public void setStripPrefix(int stripPrefix) {
      this.stripPrefix = stripPrefix;
    }

    public Resilience getResilience() {
      return resilience;
    }

    public void setResilience(Resilience resilience) {
      this.resilience = (resilience == null) ? new Resilience() : resilience;
    }

    public void validate(String key) {
      if (!StringUtils.hasText(id)) {
        throw new IllegalStateException("gateway.routes." + key + ".id must not be blank");
      }
      if (uri == null) {
        throw new IllegalStateException("gateway.routes." + key + ".uri must be provided");
      }
      if (paths == null || paths.isEmpty()) {
        throw new IllegalStateException("gateway.routes." + key + ".paths must not be empty");
      }
      for (String method : methods) {
        if (!StringUtils.hasText(method)) {
          continue;
        }
        String normalized = method.trim().toUpperCase(Locale.ROOT);
        boolean supported = Arrays.stream(HttpMethod.values())
            .anyMatch(candidate -> candidate.name().equals(normalized));
        if (!supported) {
          throw new IllegalStateException(
              "gateway.routes." + key + ".methods contains unsupported HTTP method '" + method + "'");
        }
      }
      resilience.validate(key, id);
    }

    @Override
    public String toString() {
      return "ServiceRoute{"
          + "id='" + id + '\''
          + ", uri=" + uri
          + ", paths=" + paths
          + ", methods=" + methods
          + ", stripPrefix=" + stripPrefix
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ServiceRoute that)) {
        return false;
      }
      return stripPrefix == that.stripPrefix
          && Objects.equals(id, that.id)
          && Objects.equals(uri, that.uri)
          && Objects.equals(paths, that.paths)
          && Objects.equals(methods, that.methods)
          && Objects.equals(resilience, that.resilience);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, uri, paths, methods, stripPrefix, resilience);
    }

    /**
     * Resilience configuration for a route. When enabled, the gateway will apply a
     * circuit breaker filter with an optional fallback URI.
     */
    public static class Resilience {

      private boolean enabled;

      /** Optional override for the circuit breaker name. */
      private String circuitBreakerName;

      /** Optional custom fallback URI; defaults to forwarding to the gateway fallback handler. */
      private String fallbackUri;

      /** HTTP status the filter should apply when falling back. */
      private HttpStatus fallbackStatus = HttpStatus.SERVICE_UNAVAILABLE;

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public String getCircuitBreakerName() {
        return circuitBreakerName;
      }

      public void setCircuitBreakerName(String circuitBreakerName) {
        this.circuitBreakerName = circuitBreakerName;
      }

      public String getFallbackUri() {
        return fallbackUri;
      }

      public void setFallbackUri(String fallbackUri) {
        this.fallbackUri = fallbackUri;
      }

      public HttpStatus getFallbackStatus() {
        return fallbackStatus;
      }

      public void setFallbackStatus(HttpStatus fallbackStatus) {
        this.fallbackStatus = fallbackStatus;
      }

      void validate(String key, String routeId) {
        if (!enabled) {
          return;
        }

        if (fallbackStatus == null) {
          throw new IllegalStateException("gateway.routes." + key + ".resilience.fallback-status must not be null");
        }
      }

      public String resolvedCircuitBreakerName(String routeId) {
        return StringUtils.hasText(circuitBreakerName) ? circuitBreakerName : routeId;
      }

      public String resolvedFallbackUri(String routeId) {
        if (StringUtils.hasText(fallbackUri)) {
          return fallbackUri;
        }
        return "forward:/fallback/" + routeId;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) {
          return true;
        }
        if (!(o instanceof Resilience that)) {
          return false;
        }
        return enabled == that.enabled
            && Objects.equals(circuitBreakerName, that.circuitBreakerName)
            && Objects.equals(fallbackUri, that.fallbackUri)
            && fallbackStatus == that.fallbackStatus;
      }

      @Override
      public int hashCode() {
        return Objects.hash(enabled, circuitBreakerName, fallbackUri, fallbackStatus);
      }
    }
  }
}
