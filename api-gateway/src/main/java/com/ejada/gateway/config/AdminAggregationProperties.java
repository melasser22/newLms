package com.ejada.gateway.config;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration backing the aggregated admin endpoints exposed by the gateway. Operators can
 * describe which downstream services should be queried for health/status information and how long
 * the gateway should wait for those calls before falling back to graceful degradation.
 */
@ConfigurationProperties(prefix = "gateway.admin")
public class AdminAggregationProperties {

  private Aggregation aggregation = new Aggregation();

  public Aggregation getAggregation() {
    return aggregation;
  }

  public void setAggregation(Aggregation aggregation) {
    this.aggregation = (aggregation == null) ? new Aggregation() : aggregation;
  }

  public static class Aggregation {

    private Duration timeout = Duration.ofSeconds(3);

    private List<Service> services = new ArrayList<>();

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      this.timeout = timeout;
    }

    public List<Service> getServices() {
      return services;
    }

    public void setServices(List<Service> services) {
      this.services = (services == null) ? new ArrayList<>() : new ArrayList<>(services);
    }
  }

  public static class Service {

    private String id;
    private URI uri;
    private String healthPath = "/actuator/health";
    private Duration timeout;
    private Map<String, String> headers = new LinkedHashMap<>();
    private boolean required = true;
    private String deployment = "primary";

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

    public String getHealthPath() {
      return healthPath;
    }

    public void setHealthPath(String healthPath) {
      if (!StringUtils.hasText(healthPath)) {
        this.healthPath = "/actuator/health";
      } else {
        String trimmed = healthPath.trim();
        this.healthPath = trimmed.startsWith("/") ? trimmed : '/' + trimmed;
      }
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
      if (headers == null) {
        this.headers = new LinkedHashMap<>();
        return;
      }
      Map<String, String> sanitized = new LinkedHashMap<>();
      headers.forEach((key, value) -> {
        if (!StringUtils.hasText(key)) {
          return;
        }
        String headerValue = Objects.toString(value, null);
        if (!StringUtils.hasText(headerValue)) {
          return;
        }
        sanitized.put(key.trim(), headerValue.trim());
      });
      this.headers = sanitized;
    }

    public boolean isRequired() {
      return required;
    }

    public void setRequired(boolean required) {
      this.required = required;
    }

    public String getDeployment() {
      return deployment;
    }

    public void setDeployment(String deployment) {
      this.deployment = StringUtils.hasText(deployment) ? deployment.trim() : "primary";
    }

    public Duration resolveTimeout(Duration defaultTimeout) {
      if (timeout != null && !timeout.isNegative() && !timeout.isZero()) {
        return timeout;
      }
      return (defaultTimeout == null || defaultTimeout.isNegative() || defaultTimeout.isZero())
          ? Duration.ofSeconds(3)
          : defaultTimeout;
    }

    public void validate(String key) {
      if (!StringUtils.hasText(id)) {
        throw new IllegalStateException("gateway.admin.aggregation.services[" + key + "].id must not be blank");
      }
      if (uri == null) {
        throw new IllegalStateException("gateway.admin.aggregation.services[" + key + "].uri must be provided");
      }
    }
  }
}
