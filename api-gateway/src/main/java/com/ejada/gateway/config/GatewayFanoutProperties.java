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
 * Configuration for fan-out (request splitting) behaviour. Fan-out routes trigger asynchronous
 * notifications to secondary services without impacting the primary request flow.
 */
@ConfigurationProperties(prefix = "gateway.fanout")
public class GatewayFanoutProperties {

  private boolean enabled = false;

  private Map<String, FanoutRoute> routes = new LinkedHashMap<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Map<String, FanoutRoute> getRoutes() {
    return routes;
  }

  public void setRoutes(Map<String, FanoutRoute> routes) {
    this.routes = (routes == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(routes);
  }

  public Optional<FanoutRoute> findRoute(String id) {
    if (!StringUtils.hasText(id)) {
      return Optional.empty();
    }
    return Optional.ofNullable(routes.get(id.trim()));
  }

  /** Route definition describing the fan-out targets for a specific gateway route. */
  public static class FanoutRoute {

    private List<Target> targets = new ArrayList<>();

    public List<Target> getTargets() {
      return targets;
    }

    public void setTargets(List<Target> targets) {
      this.targets = (targets == null) ? new ArrayList<>() : new ArrayList<>(targets);
    }

    public void validate(String routeId) {
      if (targets.isEmpty()) {
        throw new IllegalStateException("gateway.fanout.routes." + routeId
            + ".targets must contain at least one entry");
      }
      for (int i = 0; i < targets.size(); i++) {
        targets.get(i).validate(routeId, i);
      }
    }
  }

  /** Target service invoked asynchronously as part of a fan-out route. */
  public static class Target {

    private String id;

    private URI uri;

    private HttpMethod method = HttpMethod.POST;

    private Map<String, String> headers = new LinkedHashMap<>();

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
      if (!StringUtils.hasText(value)) {
        this.uri = null;
        return;
      }
      this.uri = URI.create(value.trim());
    }

    public HttpMethod getMethod() {
      return method;
    }

    public void setMethod(HttpMethod method) {
      this.method = (method == null) ? HttpMethod.POST : method;
    }

    public void setMethod(String method) {
      if (!StringUtils.hasText(method)) {
        this.method = HttpMethod.POST;
        return;
      }
      this.method = HttpMethod.valueOf(method.trim().toUpperCase(Locale.ROOT));
    }

    public Map<String, String> getHeaders() {
      return headers;
    }

    public void setHeaders(Map<String, String> headers) {
      this.headers = (headers == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
    }

    void validate(String routeId, int index) {
      if (!StringUtils.hasText(id)) {
        throw new IllegalStateException("gateway.fanout.routes." + routeId
            + ".targets[" + index + "].id must be provided");
      }
      if (uri == null) {
        throw new IllegalStateException("gateway.fanout.routes." + routeId
            + ".targets[" + index + "].uri must be provided");
      }
    }
  }
}

