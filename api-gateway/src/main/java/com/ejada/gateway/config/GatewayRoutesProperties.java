package com.ejada.gateway.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
      this.methods = (methods == null) ? new ArrayList<>() : new ArrayList<>(methods);
    }

    public int getStripPrefix() {
      return stripPrefix;
    }

    public void setStripPrefix(int stripPrefix) {
      this.stripPrefix = stripPrefix;
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
          && Objects.equals(methods, that.methods);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, uri, paths, methods, stripPrefix);
    }
  }
}
