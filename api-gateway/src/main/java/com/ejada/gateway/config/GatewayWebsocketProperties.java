package com.ejada.gateway.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

/**
 * Configuration for WebSocket proxy routes. These routes are handled separately from standard HTTP
 * routes so we can enforce tenant-aware connection limits.
 */
@ConfigurationProperties(prefix = "gateway.websocket")
public class GatewayWebsocketProperties {

  private boolean enabled = false;

  private int maxConnectionsPerTenant = 100;

  private Map<String, WebsocketRoute> routes = new LinkedHashMap<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getMaxConnectionsPerTenant() {
    return maxConnectionsPerTenant;
  }

  public void setMaxConnectionsPerTenant(int maxConnectionsPerTenant) {
    if (maxConnectionsPerTenant > 0) {
      this.maxConnectionsPerTenant = maxConnectionsPerTenant;
    }
  }

  public Map<String, WebsocketRoute> getRoutes() {
    return routes;
  }

  public void setRoutes(Map<String, WebsocketRoute> routes) {
    this.routes = (routes == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(routes);
  }

  public Optional<WebsocketRoute> findRoute(String id) {
    if (!StringUtils.hasText(id)) {
      return Optional.empty();
    }
    return Optional.ofNullable(routes.get(id.trim()));
  }

  /** WebSocket route definition. */
  public static class WebsocketRoute {

    private String id;

    private URI uri;

    private List<String> paths = new ArrayList<>();

    private HttpMethod method = HttpMethod.GET;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = StringUtils.hasText(id) ? id.trim() : null;
    }

    public URI getUri() {
      return uri;
    }

    public void setUri(URI uri) {
      this.uri = uri;
    }

    public void setUri(String value) {
      this.uri = StringUtils.hasText(value) ? URI.create(value.trim()) : null;
    }

    public List<String> getPaths() {
      return paths;
    }

    public void setPaths(List<String> paths) {
      this.paths = (paths == null) ? new ArrayList<>() : new ArrayList<>(paths);
    }

    public HttpMethod getMethod() {
      return method;
    }

    public void setMethod(HttpMethod method) {
      this.method = (method == null) ? HttpMethod.GET : method;
    }

    public void setMethod(String method) {
      if (!StringUtils.hasText(method)) {
        this.method = HttpMethod.GET;
        return;
      }
      this.method = HttpMethod.valueOf(method.trim().toUpperCase(Locale.ROOT));
    }

    public void validate(String key) {
      if (!StringUtils.hasText(id)) {
        throw new IllegalStateException("gateway.websocket.routes." + key + ".id must be provided");
      }
      if (uri == null) {
        throw new IllegalStateException("gateway.websocket.routes." + key + ".uri must be provided");
      }
      if (paths.isEmpty()) {
        throw new IllegalStateException("gateway.websocket.routes." + key + ".paths must not be empty");
      }
    }
  }
}

