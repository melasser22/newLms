package com.ejada.gateway.config;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

/**
 * Configuration for request aggregation (backend-for-frontend) routes. Each aggregation route is
 * keyed by the logical gateway route identifier and lists downstream requests that should be
 * fan-out/fan-in to compose the response returned to the client.
 */
@ConfigurationProperties(prefix = "gateway.aggregation")
public class GatewayAggregationProperties {

  private boolean enabled = false;

  private Map<String, AggregationRoute> routes = new LinkedHashMap<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Map<String, AggregationRoute> getRoutes() {
    return routes;
  }

  public void setRoutes(Map<String, AggregationRoute> routes) {
    this.routes = (routes == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(routes);
  }

  public Optional<AggregationRoute> findRoute(String id) {
    if (!StringUtils.hasText(id)) {
      return Optional.empty();
    }
    return Optional.ofNullable(routes.get(id.trim()));
  }

  /**
   * Aggregation route definition containing downstream calls to execute.
   */
  public static class AggregationRoute {

    private Duration timeout = Duration.ofSeconds(10);

    private List<UpstreamRequest> upstreamRequests = new ArrayList<>();

    private boolean includeErrorDetails = false;

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      if (timeout == null || timeout.isNegative() || timeout.isZero()) {
        return;
      }
      this.timeout = timeout;
    }

    public List<UpstreamRequest> getUpstreamRequests() {
      return upstreamRequests;
    }

    public void setUpstreamRequests(List<UpstreamRequest> upstreamRequests) {
      this.upstreamRequests = (upstreamRequests == null)
          ? new ArrayList<>()
          : new ArrayList<>(upstreamRequests);
    }

    public boolean isIncludeErrorDetails() {
      return includeErrorDetails;
    }

    public void setIncludeErrorDetails(boolean includeErrorDetails) {
      this.includeErrorDetails = includeErrorDetails;
    }

    public void validate(String routeId) {
      Objects.requireNonNull(routeId, "routeId");
      if (upstreamRequests.isEmpty()) {
        throw new IllegalStateException("gateway.aggregation.routes." + routeId
            + ".upstream-requests must contain at least one entry");
      }
      for (int i = 0; i < upstreamRequests.size(); i++) {
        upstreamRequests.get(i).validate(routeId, i);
      }
    }
  }

  /**
   * Downstream request definition executed as part of an aggregation route.
   */
  public static class UpstreamRequest {

    private String id;

    private URI uri;

    private String uriTemplate;

    private HttpMethod method = HttpMethod.GET;

    private Duration timeout;

    private Map<String, String> headers = new LinkedHashMap<>();

    private String circuitBreakerName;

    private boolean optional = false;

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
      this.uriTemplate = (uri == null) ? null : uri.toString();
    }

    public void setUri(String value) {
      if (!StringUtils.hasText(value)) {
        this.uri = null;
        this.uriTemplate = null;
        return;
      }
      String trimmed = value.trim();
      this.uriTemplate = trimmed;
      if (trimmed.contains("{") || trimmed.contains("}")) {
        this.uri = null;
      } else {
        this.uri = URI.create(trimmed);
      }
    }

    public String getUriTemplate() {
      return uriTemplate;
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

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      this.timeout = timeout;
    }

    public Map<String, String> getHeaders() {
      return headers;
    }

    public void setHeaders(Map<String, String> headers) {
      this.headers = (headers == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
    }

    public String getCircuitBreakerName() {
      return circuitBreakerName;
    }

    public void setCircuitBreakerName(String circuitBreakerName) {
      this.circuitBreakerName = StringUtils.hasText(circuitBreakerName)
          ? circuitBreakerName.trim()
          : null;
    }

    public boolean isOptional() {
      return optional;
    }

    public void setOptional(boolean optional) {
      this.optional = optional;
    }

    public Duration resolveTimeout(Duration defaultTimeout) {
      if (timeout == null || timeout.isNegative() || timeout.isZero()) {
        return defaultTimeout;
      }
      return timeout;
    }

    void validate(String routeId, int index) {
      if (!StringUtils.hasText(id)) {
        throw new IllegalStateException("gateway.aggregation.routes." + routeId
            + ".upstream-requests[" + index + "].id must be provided");
      }
      if (!StringUtils.hasText(uriTemplate) && uri == null) {
        throw new IllegalStateException("gateway.aggregation.routes." + routeId
            + ".upstream-requests[" + index + "].uri must be provided");
      }
    }
  }
}

