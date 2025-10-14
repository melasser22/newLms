package com.ejada.gateway.config;

import java.net.URI;
import java.time.Duration;
import com.ejada.gateway.versioning.VersionNumber;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties describing the downstream services that should be
 * exposed through the gateway. The structure intentionally mirrors the
 * documented plan so operators can declaratively manage routes per
 * environment.
 */
@RefreshScope
@Validated
@ConfigurationProperties(prefix = "gateway")
public class GatewayRoutesProperties {

  private final Map<String, ServiceRoute> routes = new LinkedHashMap<>();

  private RouteDefaults defaults = new RouteDefaults();

  public Map<String, ServiceRoute> getRoutes() {
    return routes;
  }

  public RouteDefaults getDefaults() {
    return defaults;
  }

  public void setDefaults(RouteDefaults defaults) {
    this.defaults = (defaults == null) ? new RouteDefaults() : defaults;
  }

  public Optional<ServiceRoute> findRouteById(String id) {
    if (!StringUtils.hasText(id)) {
      return Optional.empty();
    }
    return routes.values().stream()
        .filter(Objects::nonNull)
        .filter(route -> id.equals(route.getId()))
        .findFirst();
  }

  /**
   * Optional defaults that can be merged into every declared route. Primarily used to
   * avoid repeating resilience fallback values.
   */
  public static class RouteDefaults {

    private ServiceRoute.Resilience resilience = new ServiceRoute.Resilience();

    public ServiceRoute.Resilience getResilience() {
      return resilience;
    }

    public void setResilience(ServiceRoute.Resilience resilience) {
      this.resilience = (resilience == null) ? new ServiceRoute.Resilience() : resilience;
    }
  }

  /**
   * Route definition for a downstream service.
   */
  public static class ServiceRoute {

    /** Identifier used for RouteLocator registration. */
    private String id;

    /** Optional logical variant identifier when route participates in traffic splits. */
    private String variantId;

    /** Downstream service URI (e.g. http://tenant-service:8080). */
    private URI uri;

    /** Paths that should map to the downstream service. */
    private List<String> paths = new ArrayList<>();

    /** Optional HTTP methods to restrict the route (empty = all). */
    private List<String> methods = new ArrayList<>();

    /** Number of path segments to strip before forwarding. */
    private int stripPrefix = 1;

    /** Optional static path prefix to prepend before forwarding. */
    private String prefixPath;

    /** Optional resilience configuration for the route. */
    private Resilience resilience = new Resilience();

    /** Optional request headers to automatically append before forwarding. */
    private Map<String, String> requestHeaders = new LinkedHashMap<>();

    /** Optional tenant specific routing directives. */
    private Map<String, TenantRoutingRule> tenantRouting = new LinkedHashMap<>();

    /** Optional API version handling configuration. */
    private Versioning versioning = new Versioning();

    /** Optional traffic weighting (used for blue/green or canary deployments). */
    private Weight weight = new Weight();

    /** Session affinity preferences for stateful downstream services. */
    private SessionAffinity sessionAffinity = new SessionAffinity();

    /** Load balancing strategy to apply when forwarding traffic to this route. */
    private LoadBalancingStrategy lbStrategy = LoadBalancingStrategy.DEFAULT;

    /** Required scopes that must be present on API keys accessing this route. */
    private Set<String> requiredScopes = new LinkedHashSet<>();

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getVariantId() {
      return variantId;
    }

    public void setVariantId(String variantId) {
      this.variantId = (variantId == null) ? null : variantId.trim();
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

    public String getPrefixPath() {
      return prefixPath;
    }

    public void setPrefixPath(String prefixPath) {
      if (!StringUtils.hasText(prefixPath)) {
        this.prefixPath = null;
        return;
      }

      String value = prefixPath.trim();
      if (!value.startsWith("/")) {
        value = '/' + value;
      }
      if (value.length() > 1 && value.endsWith("/")) {
        value = value.substring(0, value.length() - 1);
      }

      this.prefixPath = value;
    }

    public Resilience getResilience() {
      return resilience;
    }

    public void setResilience(Resilience resilience) {
      this.resilience = (resilience == null) ? new Resilience() : resilience;
    }

    public Map<String, String> getRequestHeaders() {
      return requestHeaders;
    }

    public void setRequestHeaders(Map<String, String> requestHeaders) {
      this.requestHeaders = sanitiseHeaders(requestHeaders);
    }

    public Map<String, TenantRoutingRule> getTenantRouting() {
      return tenantRouting;
    }

    public void setTenantRouting(Map<String, TenantRoutingRule> tenantRouting) {
      this.tenantRouting = (tenantRouting == null)
          ? new LinkedHashMap<>()
          : tenantRouting.entrySet().stream()
              .filter(entry -> StringUtils.hasText(entry.getKey()) && entry.getValue() != null)
              .collect(Collectors.toMap(entry -> entry.getKey().trim(),
                  entry -> entry.getValue().copy(),
                  (left, right) -> right,
                  LinkedHashMap::new));
    }

    public Versioning getVersioning() {
      return versioning;
    }

    public void setVersioning(Versioning versioning) {
      this.versioning = (versioning == null) ? new Versioning() : versioning;
    }

    public Weight getWeight() {
      return weight;
    }

    public void setWeight(Weight weight) {
      this.weight = (weight == null) ? new Weight() : weight;
    }

    public SessionAffinity getSessionAffinity() {
      return sessionAffinity;
    }

    public void setSessionAffinity(SessionAffinity sessionAffinity) {
      this.sessionAffinity = (sessionAffinity == null) ? new SessionAffinity() : sessionAffinity;
    }

    public LoadBalancingStrategy getLbStrategy() {
      return lbStrategy;
    }

    public void setLbStrategy(LoadBalancingStrategy lbStrategy) {
      this.lbStrategy = (lbStrategy == null) ? LoadBalancingStrategy.DEFAULT : lbStrategy;
    }

    public void setLbStrategy(String lbStrategy) {
      this.lbStrategy = LoadBalancingStrategy.from(lbStrategy);
    }

    public Set<String> getRequiredScopes() {
      return requiredScopes;
    }

    public void setRequiredScopes(Collection<String> scopes) {
      if (scopes == null || scopes.isEmpty()) {
        this.requiredScopes = new LinkedHashSet<>();
        return;
      }
      this.requiredScopes = scopes.stream()
          .filter(StringUtils::hasText)
          .map(String::trim)
          .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void setRequiredScopes(String... scopes) {
      if (scopes == null) {
        setRequiredScopes((Collection<String>) null);
        return;
      }
      setRequiredScopes(Arrays.asList(scopes));
    }

    public void applyDefaults(RouteDefaults defaults) {
      if (defaults == null) {
        return;
      }
      if (this.resilience == null) {
        this.resilience = new Resilience();
      }
      this.resilience.applyDefaults(defaults.getResilience());
    }

    /**
     * Tenant-specific routing directives that can steer certain tenants to dedicated instances or
     * adjust relative weights.
     */
    public static class TenantRoutingRule {

      private List<String> dedicatedInstances = new ArrayList<>();
      private Map<String, Integer> instanceWeights = new LinkedHashMap<>();

      public List<String> getDedicatedInstances() {
        return dedicatedInstances;
      }

      public void setDedicatedInstances(List<String> dedicatedInstances) {
        if (dedicatedInstances == null) {
          this.dedicatedInstances = new ArrayList<>();
          return;
        }
        this.dedicatedInstances = dedicatedInstances.stream()
            .filter(StringUtils::hasText)
            .map(value -> value.trim().toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(ArrayList::new));
      }

      public Map<String, Integer> getInstanceWeights() {
        return instanceWeights;
      }

      public void setInstanceWeights(Map<String, Integer> instanceWeights) {
        if (instanceWeights == null) {
          this.instanceWeights = new LinkedHashMap<>();
          return;
        }
        LinkedHashMap<String, Integer> sanitized = new LinkedHashMap<>();
        instanceWeights.forEach((instanceId, weight) -> {
          if (!StringUtils.hasText(instanceId) || weight == null) {
            return;
          }
          sanitized.put(instanceId.trim(), weight);
        });
        this.instanceWeights = sanitized;
      }

      void validate(String key, String tenantId) {
        for (String instance : dedicatedInstances) {
          if (!StringUtils.hasText(instance)) {
            throw new IllegalStateException("gateway.routes." + key + ".tenant-routing." + tenantId
                + ".dedicated-instances contains a blank entry");
          }
        }
        for (Map.Entry<String, Integer> entry : instanceWeights.entrySet()) {
          String instance = entry.getKey();
          Integer weight = entry.getValue();
          if (!StringUtils.hasText(instance)) {
            throw new IllegalStateException("gateway.routes." + key + ".tenant-routing." + tenantId
                + ".instance-weights contains a blank instance id");
          }
          if (weight == null || weight <= 0) {
            throw new IllegalStateException("gateway.routes." + key + ".tenant-routing." + tenantId
                + ".instance-weights." + instance + " must be positive");
          }
        }
      }

      TenantRoutingRule copy() {
        TenantRoutingRule copy = new TenantRoutingRule();
        copy.setDedicatedInstances(dedicatedInstances);
        copy.setInstanceWeights(instanceWeights);
        return copy;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) {
          return true;
        }
        if (!(o instanceof TenantRoutingRule that)) {
          return false;
        }
        return Objects.equals(dedicatedInstances, that.dedicatedInstances)
            && Objects.equals(instanceWeights, that.instanceWeights);
      }

      @Override
      public int hashCode() {
        return Objects.hash(dedicatedInstances, instanceWeights);
      }

      @Override
      public String toString() {
        return "TenantRoutingRule{" +
            "dedicatedInstances=" + dedicatedInstances +
            ", instanceWeights=" + instanceWeights +
            '}';
      }
    }

    private Map<String, String> sanitiseHeaders(Map<String, String> headers) {
      if (headers == null || headers.isEmpty()) {
        return new LinkedHashMap<>();
      }
      Map<String, String> sanitized = new LinkedHashMap<>();
      headers.forEach((key, value) -> {
        if (!StringUtils.hasText(key)) {
          return;
        }
        String headerValue = Objects.toString(value, null);
        if (headerValue == null) {
          return;
        }
        sanitized.put(key.trim(), headerValue.trim());
      });
      return sanitized;
    }

    private void validateHeaders(String key) {
      for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
        String headerName = entry.getKey();
        if (!StringUtils.hasText(headerName)) {
          throw new IllegalStateException("gateway.routes." + key + ".request-headers contains a blank header name");
        }
        if (!StringUtils.hasText(entry.getValue())) {
          throw new IllegalStateException(
              "gateway.routes." + key + ".request-headers." + headerName + " must have a value");
        }
      }
      for (String scope : requiredScopes) {
        if (!StringUtils.hasText(scope)) {
          throw new IllegalStateException("gateway.routes." + key + ".required-scopes contains a blank scope entry");
        }
      }
    }

    private void validateTenantRouting(String key) {
      for (Map.Entry<String, TenantRoutingRule> entry : tenantRouting.entrySet()) {
        String tenantId = entry.getKey();
        if (!StringUtils.hasText(tenantId)) {
          throw new IllegalStateException("gateway.routes." + key + ".tenant-routing contains a blank tenant id");
        }
        TenantRoutingRule rule = entry.getValue();
        if (rule == null) {
          continue;
        }
        rule.validate(key, tenantId.trim());
      }
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
      if (StringUtils.hasText(prefixPath) && !prefixPath.startsWith("/")) {
        throw new IllegalStateException(
            "gateway.routes." + key + ".prefixPath must start with '/' if provided");
      }
      resilience.validate(key);
      versioning.validate(key);
      weight.validate(key);
      sessionAffinity.validate(key);
      validateHeaders(key);
      validateTenantRouting(key);
    }

    @Override
    public String toString() {
      return "ServiceRoute{"
          + "id='" + id + '\''
          + ", variantId='" + variantId + '\''
          + ", uri=" + uri
          + ", paths=" + paths
          + ", methods=" + methods
          + ", stripPrefix=" + stripPrefix
          + ", prefixPath='" + prefixPath + '\''
          + ", requestHeaders=" + requestHeaders
          + ", tenantRouting=" + tenantRouting
          + ", versioning=" + versioning
          + ", weight=" + weight
          + ", sessionAffinity=" + sessionAffinity
          + ", lbStrategy=" + lbStrategy
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
          && Objects.equals(variantId, that.variantId)
          && Objects.equals(uri, that.uri)
          && Objects.equals(paths, that.paths)
          && Objects.equals(methods, that.methods)
          && Objects.equals(prefixPath, that.prefixPath)
          && Objects.equals(resilience, that.resilience)
          && Objects.equals(requestHeaders, that.requestHeaders)
          && Objects.equals(tenantRouting, that.tenantRouting)
          && Objects.equals(versioning, that.versioning)
          && Objects.equals(weight, that.weight)
          && Objects.equals(sessionAffinity, that.sessionAffinity)
          && lbStrategy == that.lbStrategy;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, variantId, uri, paths, methods, stripPrefix, prefixPath, resilience,
          requestHeaders, tenantRouting, versioning, weight, sessionAffinity, lbStrategy);
    }

    /**
     * Controls how the gateway interprets and propagates API version segments for a
     * particular route.
     */
    public static class Versioning {

      private boolean enabled;
      private String defaultVersion = "v1";
      private List<String> supportedVersions = new ArrayList<>();
      private boolean fallbackToDefault = true;
      private boolean propagateHeader = true;

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public String getDefaultVersion() {
        return defaultVersion;
      }

      public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = (defaultVersion == null) ? null : defaultVersion.trim();
      }

      public List<String> getSupportedVersions() {
        return Collections.unmodifiableList(supportedVersions);
      }

      public void setSupportedVersions(List<String> supportedVersions) {
        if (supportedVersions == null) {
          this.supportedVersions = new ArrayList<>();
          return;
        }
        LinkedHashSet<String> deduped = supportedVersions.stream()
            .filter(StringUtils::hasText)
            .map(value -> value.trim().toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        this.supportedVersions = new ArrayList<>(deduped);
      }

      public boolean isFallbackToDefault() {
        return fallbackToDefault;
      }

      public void setFallbackToDefault(boolean fallbackToDefault) {
        this.fallbackToDefault = fallbackToDefault;
      }

      public boolean isPropagateHeader() {
        return propagateHeader;
      }

      public void setPropagateHeader(boolean propagateHeader) {
        this.propagateHeader = propagateHeader;
      }

      public boolean hasSupportedVersions() {
        return !supportedVersions.isEmpty();
      }

      void validate(String key) {
        if (!enabled) {
          return;
        }
        defaultVersion = canonicalise(defaultVersion, key, "default-version");
        List<String> canonical = new ArrayList<>();
        for (String version : supportedVersions) {
          canonical.add(canonicalise(version, key, "supported-versions"));
        }
        LinkedHashSet<String> deduped = new LinkedHashSet<>(canonical);
        deduped.add(defaultVersion);
        supportedVersions = new ArrayList<>(deduped);
      }

      private String canonicalise(String value, String key, String property) {
        if (!StringUtils.hasText(value)) {
          throw new IllegalStateException(
              "gateway.routes." + key + ".versioning." + property + " must not be blank when versioning is enabled");
        }
        try {
          return VersionNumber.canonicalise(value);
        } catch (IllegalArgumentException ex) {
          throw new IllegalStateException(
              "gateway.routes." + key + ".versioning." + property + " must be a numeric version (e.g. v1, 2.0)",
              ex);
        }
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) {
          return true;
        }
        if (!(o instanceof Versioning versioning)) {
          return false;
        }
        return enabled == versioning.enabled
            && fallbackToDefault == versioning.fallbackToDefault
            && propagateHeader == versioning.propagateHeader
            && Objects.equals(defaultVersion, versioning.defaultVersion)
            && Objects.equals(supportedVersions, versioning.supportedVersions);
      }

      @Override
      public int hashCode() {
        return Objects.hash(enabled, defaultVersion, supportedVersions, fallbackToDefault, propagateHeader);
      }

      @Override
      public String toString() {
        return "Versioning{" +
            "enabled=" + enabled +
            ", defaultVersion='" + defaultVersion + '\'' +
            ", supportedVersions=" + supportedVersions +
            ", fallbackToDefault=" + fallbackToDefault +
            ", propagateHeader=" + propagateHeader +
            '}';
      }
    }

    /**
     * Weight configuration used to split traffic between multiple routes that share the same
     * weight group. Enables canary or blue/green releases without redeploying the gateway.
     */
    public static class Weight {

      private boolean enabled;
      private String group;
      private int value = 100;

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public String getGroup() {
        return group;
      }

      public void setGroup(String group) {
        this.group = (group == null) ? null : group.trim();
      }

      public int getValue() {
        return value;
      }

      public void setValue(int value) {
        this.value = value;
      }

      void validate(String key) {
        if (!enabled) {
          return;
        }
        if (!StringUtils.hasText(group)) {
          throw new IllegalStateException(
              "gateway.routes." + key + ".weight.group must be provided when weight is enabled");
        }
        if (value <= 0) {
          throw new IllegalStateException(
              "gateway.routes." + key + ".weight.value must be a positive integer");
        }
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) {
          return true;
        }
        if (!(o instanceof Weight weight)) {
          return false;
        }
        return enabled == weight.enabled
            && value == weight.value
            && Objects.equals(group, weight.group);
      }

      @Override
      public int hashCode() {
        return Objects.hash(enabled, group, value);
      }

      @Override
      public String toString() {
        return "Weight{" +
            "enabled=" + enabled +
            ", group='" + group + '\'' +
            ", value=" + value +
            '}';
      }
    }

    /**
     * Session affinity preferences used when downstream services maintain conversational state.
     */
    public static class SessionAffinity {

      private boolean enabled;
      private String cookieName = "LMS-AFFINITY";
      private String headerName = "X-Session-Affinity";

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public String getCookieName() {
        return cookieName;
      }

      public void setCookieName(String cookieName) {
        this.cookieName = (cookieName == null || cookieName.isBlank()) ? "LMS-AFFINITY" : cookieName.trim();
      }

      public String getHeaderName() {
        return headerName;
      }

      public void setHeaderName(String headerName) {
        this.headerName = (headerName == null || headerName.isBlank()) ? "X-Session-Affinity" : headerName.trim();
      }

      void validate(String key) {
        if (!enabled) {
          return;
        }
        if (!StringUtils.hasText(cookieName)) {
          cookieName = "LMS-AFFINITY";
        }
        if (!StringUtils.hasText(headerName)) {
          headerName = "X-Session-Affinity";
        }
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) {
          return true;
        }
        if (!(o instanceof SessionAffinity that)) {
          return false;
        }
        return enabled == that.enabled
            && Objects.equals(cookieName, that.cookieName)
            && Objects.equals(headerName, that.headerName);
      }

      @Override
      public int hashCode() {
        return Objects.hash(enabled, cookieName, headerName);
      }

      @Override
      public String toString() {
        return "SessionAffinity{" +
            "enabled=" + enabled +
            ", cookieName='" + cookieName + '\'' +
            ", headerName='" + headerName + '\'' +
            '}';
      }
    }

    public enum LoadBalancingStrategy {
      DEFAULT,
      WEIGHTED_RESPONSE_TIME;

      static LoadBalancingStrategy from(String value) {
        if (!StringUtils.hasText(value)) {
          return DEFAULT;
        }
        try {
          return LoadBalancingStrategy.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
          throw new IllegalStateException("Unsupported load balancing strategy: " + value, ex);
        }
      }
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

      /** Optional override for the fallback message returned from the gateway. */
      private String fallbackMessage;

      /** Optional retry configuration when invoking the downstream service. */
      private Retry retry = new Retry();

      private Set<HttpMethod> fallbackOnMethods = new LinkedHashSet<>();

      private Priority priority = Priority.NON_CRITICAL;

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

      public String getFallbackMessage() {
        return fallbackMessage;
      }

      public void setFallbackMessage(String fallbackMessage) {
        this.fallbackMessage = fallbackMessage;
      }

      public Retry getRetry() {
        return retry;
      }

      public void setRetry(Retry retry) {
        this.retry = (retry == null) ? new Retry() : retry;
      }

      public Priority getPriority() {
        return (priority == null) ? Priority.NON_CRITICAL : priority;
      }

      public void setPriority(Priority priority) {
        this.priority = (priority == null) ? Priority.NON_CRITICAL : priority;
      }

      public Set<HttpMethod> getFallbackOnMethods() {
        return Collections.unmodifiableSet(fallbackOnMethods);
      }

      public void setFallbackOnMethods(Collection<HttpMethod> fallbackOnMethods) {
        Set<HttpMethod> methods = new LinkedHashSet<>();
        if (fallbackOnMethods != null) {
          fallbackOnMethods.stream()
              .filter(Objects::nonNull)
              .forEach(methods::add);
        }
        this.fallbackOnMethods = methods;
      }

      public void applyDefaults(Resilience defaults) {
        if (defaults == null) {
          return;
        }
        if (!this.enabled) {
          this.enabled = defaults.enabled;
        }
        if (!StringUtils.hasText(this.circuitBreakerName)) {
          this.circuitBreakerName = defaults.circuitBreakerName;
        }
        if (!StringUtils.hasText(this.fallbackUri)) {
          this.fallbackUri = defaults.fallbackUri;
        }
        if (this.fallbackStatus == null && defaults.fallbackStatus != null) {
          this.fallbackStatus = defaults.fallbackStatus;
        }
        if (!StringUtils.hasText(this.fallbackMessage)) {
          this.fallbackMessage = defaults.fallbackMessage;
        }
        if (this.retry == null) {
          this.retry = new Retry();
        }
        this.retry.applyDefaults(defaults.retry);
        if (this.priority == null) {
          this.priority = defaults.priority;
        }
        if (this.fallbackOnMethods.isEmpty()
            && defaults.fallbackOnMethods != null
            && !defaults.fallbackOnMethods.isEmpty()) {
          this.fallbackOnMethods = new LinkedHashSet<>(defaults.fallbackOnMethods);
        }
      }

      void validate(String key) {
        if (!enabled) {
          return;
        }

        if (fallbackStatus == null) {
          throw new IllegalStateException("gateway.routes." + key + ".resilience.fallback-status must not be null");
        }

        retry.validate(key);
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

      public String resolvedBulkheadName(String routeId) {
        return resolvedCircuitBreakerName(routeId) + "-bulkhead";
      }

      public Optional<String> resolvedFallbackMessage() {
        return Optional.ofNullable(StringUtils.hasText(fallbackMessage) ? fallbackMessage.trim() : null);
      }

      public boolean isFallbackMethodAllowed(HttpMethod method) {
        if (fallbackOnMethods == null || fallbackOnMethods.isEmpty()) {
          return true;
        }
        return method != null && fallbackOnMethods.contains(method);
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
            && fallbackStatus == that.fallbackStatus
            && Objects.equals(fallbackMessage, that.fallbackMessage)
            && Objects.equals(retry, that.retry)
            && priority == that.priority
            && Objects.equals(fallbackOnMethods, that.fallbackOnMethods);
      }

      @Override
      public int hashCode() {
        return Objects.hash(enabled, circuitBreakerName, fallbackUri, fallbackStatus, fallbackMessage, retry,
            priority, fallbackOnMethods);
      }

      @Override
      public String toString() {
        return "Resilience{"
            + "enabled=" + enabled
            + ", circuitBreakerName='" + circuitBreakerName + '\''
            + ", fallbackUri='" + fallbackUri + '\''
            + ", fallbackStatus=" + fallbackStatus
            + ", retry=" + retry
            + ", priority=" + priority
            + ", fallbackOnMethods=" + fallbackOnMethods
            + '}';
      }

      public enum Priority {
        CRITICAL,
        NON_CRITICAL
      }

      /**
       * Declarative retry configuration compatible with Spring Cloud Gateway's
       * {@code RetryGatewayFilterFactory}.
       */
      public static class Retry {

        private boolean enabled;
        private int retries = 3;
        private List<String> statuses = new ArrayList<>();
        private List<String> series = new ArrayList<>();
        private List<String> methods = new ArrayList<>();
        private List<String> exceptions = new ArrayList<>();
        private Backoff backoff = new Backoff();

        public boolean isEnabled() {
          return enabled;
        }

        public void setEnabled(boolean enabled) {
          this.enabled = enabled;
        }

        public int getRetries() {
          return retries;
        }

        public void setRetries(int retries) {
          this.retries = retries;
        }

        public List<String> getStatuses() {
          return statuses;
        }

        public void setStatuses(List<String> statuses) {
          this.statuses = normaliseList(statuses, false);
        }

        public List<String> getSeries() {
          return series;
        }

        public void setSeries(List<String> series) {
          this.series = normaliseList(series, true);
        }

        public List<String> getMethods() {
          return methods;
        }

        public void setMethods(List<String> methods) {
          this.methods = normaliseList(methods, true);
        }

        public List<String> getExceptions() {
          return exceptions;
        }

        public void setExceptions(List<String> exceptions) {
          this.exceptions = normaliseList(exceptions, false);
        }

        public Backoff getBackoff() {
          return backoff;
        }

        public void setBackoff(Backoff backoff) {
          this.backoff = (backoff == null) ? new Backoff() : backoff;
        }

        public void applyDefaults(Retry defaults) {
          if (defaults == null) {
            return;
          }
          if (!this.enabled) {
            this.enabled = defaults.enabled;
          }
          if (this.retries <= 0) {
            this.retries = defaults.retries;
          }
          if (this.statuses.isEmpty()) {
            this.statuses = new ArrayList<>(defaults.statuses);
          }
          if (this.series.isEmpty()) {
            this.series = new ArrayList<>(defaults.series);
          }
          if (this.methods.isEmpty()) {
            this.methods = new ArrayList<>(defaults.methods);
          }
          if (this.exceptions.isEmpty()) {
            this.exceptions = new ArrayList<>(defaults.exceptions);
          }
          if (this.backoff == null) {
            this.backoff = new Backoff();
          }
          this.backoff.applyDefaults(defaults.backoff);
        }

        void validate(String key) {
          if (!enabled) {
            return;
          }

          if (retries < 1) {
            throw new IllegalStateException("gateway.routes." + key + ".resilience.retry.retries must be at least 1");
          }

          for (String status : statuses) {
            if (parseStatus(status) == null) {
              throw new IllegalStateException(
                  "gateway.routes." + key + ".resilience.retry.statuses contains invalid HTTP status '" + status + "'");
            }
          }

          for (String serie : series) {
            if (parseSeries(serie) == null) {
              throw new IllegalStateException(
                  "gateway.routes." + key + ".resilience.retry.series contains invalid HTTP status series '" + serie + "'");
            }
          }

          for (String method : methods) {
            if (!StringUtils.hasText(method)) {
              continue;
            }
            try {
              HttpMethod.valueOf(method.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
              throw new IllegalStateException(
                  "gateway.routes." + key + ".resilience.retry.methods contains unsupported HTTP method '" + method + "'",
                  ex);
            }
          }

          ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
          if (classLoader == null) {
            classLoader = Retry.class.getClassLoader();
          }
          for (String exception : exceptions) {
            if (!StringUtils.hasText(exception)) {
              continue;
            }
            String candidate = exception.trim();
            if (!ClassUtils.isPresent(candidate, classLoader)) {
              throw new IllegalStateException(
                  "gateway.routes." + key + ".resilience.retry.exceptions contains unknown class '" + candidate + "'");
            }
            Class<?> resolved = ClassUtils.resolveClassName(candidate, classLoader);
            if (!Throwable.class.isAssignableFrom(resolved)) {
              throw new IllegalStateException(
                  "gateway.routes." + key + ".resilience.retry.exceptions contains non-Throwable class '" + candidate + "'");
            }
          }

          backoff.validate(key);
        }

        public HttpStatus[] resolvedStatuses() {
          return statuses.stream()
              .map(Retry::parseStatus)
              .filter(Objects::nonNull)
              .toArray(HttpStatus[]::new);
        }

        public HttpStatus.Series[] resolvedSeries() {
          return series.stream()
              .map(Retry::parseSeries)
              .filter(Objects::nonNull)
              .toArray(HttpStatus.Series[]::new);
        }

        public HttpMethod[] resolvedMethods() {
          return methods.stream()
              .filter(StringUtils::hasText)
              .map(value -> value.trim().toUpperCase(Locale.ROOT))
              .map(HttpMethod::valueOf)
              .toArray(HttpMethod[]::new);
        }

        @SuppressWarnings("unchecked")
        public Class<? extends Throwable>[] resolvedExceptions() {
          ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
          if (classLoader == null) {
            classLoader = Retry.class.getClassLoader();
          }
          final ClassLoader classLoaderToUse = classLoader;
          java.util.List<Class<? extends Throwable>> resolved = exceptions.stream()
              .filter(StringUtils::hasText)
              .map(String::trim)
              .map(name -> {
                try {
                  return (Class<? extends Throwable>) ClassUtils.resolveClassName(name, classLoaderToUse);
                } catch (IllegalArgumentException ex) {
                  throw new IllegalStateException("Unable to resolve exception class '" + name + "'", ex);
                }
              })
              .collect(java.util.stream.Collectors.toList());
          return resolved.toArray(new Class[0]);
        }

        private static List<String> normaliseList(List<String> input, boolean uppercase) {
          if (input == null) {
            return new ArrayList<>();
          }
          var deduped = input.stream()
              .filter(StringUtils::hasText)
              .map(String::trim)
              .map(value -> uppercase ? value.toUpperCase(Locale.ROOT) : value)
              .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
          return new ArrayList<>(deduped);
        }

        private static HttpStatus parseStatus(String candidate) {
          if (!StringUtils.hasText(candidate)) {
            return null;
          }
          String value = candidate.trim();
          if (value.chars().allMatch(Character::isDigit)) {
            try {
              return HttpStatus.valueOf(Integer.parseInt(value));
            } catch (IllegalArgumentException ex) {
              return null;
            }
          }
          try {
            return HttpStatus.valueOf(value.toUpperCase(Locale.ROOT));
          } catch (IllegalArgumentException ex) {
            return null;
          }
        }

        private static HttpStatus.Series parseSeries(String candidate) {
          if (!StringUtils.hasText(candidate)) {
            return null;
          }
          try {
            return HttpStatus.Series.valueOf(candidate.trim().toUpperCase(Locale.ROOT));
          } catch (IllegalArgumentException ex) {
            return null;
          }
        }

        @Override
        public boolean equals(Object o) {
          if (this == o) {
            return true;
          }
          if (!(o instanceof Retry retry)) {
            return false;
          }
          return enabled == retry.enabled
              && retries == retry.retries
              && Objects.equals(statuses, retry.statuses)
              && Objects.equals(series, retry.series)
              && Objects.equals(methods, retry.methods)
              && Objects.equals(exceptions, retry.exceptions)
              && Objects.equals(backoff, retry.backoff);
        }

        @Override
        public int hashCode() {
          return Objects.hash(enabled, retries, statuses, series, methods, exceptions, backoff);
        }

        @Override
        public String toString() {
          return "Retry{"
              + "enabled=" + enabled
              + ", retries=" + retries
              + ", statuses=" + statuses
              + ", series=" + series
              + ", methods=" + methods
              + ", exceptions=" + exceptions
              + ", backoff=" + backoff
              + '}';
        }

        /**
         * Backoff settings applied when retrying downstream calls.
         */
        public static class Backoff {

          private boolean enabled;
          private Duration firstBackoff = Duration.ofMillis(50);
          private Duration maxBackoff = Duration.ofSeconds(2);
          private int factor = 2;
          private boolean basedOnPreviousValue = true;

          public boolean isEnabled() {
            return enabled;
          }

          public void setEnabled(boolean enabled) {
            this.enabled = enabled;
          }

          public Duration getFirstBackoff() {
            return firstBackoff;
          }

          public void setFirstBackoff(Duration firstBackoff) {
            this.firstBackoff = firstBackoff;
          }

          public Duration getMaxBackoff() {
            return maxBackoff;
          }

          public void setMaxBackoff(Duration maxBackoff) {
            this.maxBackoff = maxBackoff;
          }

          public int getFactor() {
            return factor;
          }

          public void setFactor(int factor) {
            this.factor = factor;
          }

          public boolean isBasedOnPreviousValue() {
            return basedOnPreviousValue;
          }

          public void setBasedOnPreviousValue(boolean basedOnPreviousValue) {
            this.basedOnPreviousValue = basedOnPreviousValue;
          }

          public void applyDefaults(Backoff defaults) {
            if (defaults == null) {
              return;
            }
            if (!this.enabled) {
              this.enabled = defaults.enabled;
            }
            if (this.firstBackoff == null) {
              this.firstBackoff = defaults.firstBackoff;
            }
            if (this.maxBackoff == null) {
              this.maxBackoff = defaults.maxBackoff;
            }
            if (this.factor <= 0) {
              this.factor = defaults.factor;
            }
            if (!this.basedOnPreviousValue) {
              this.basedOnPreviousValue = defaults.basedOnPreviousValue;
            }
          }

          void validate(String key) {
            if (!enabled) {
              return;
            }
            if (firstBackoff == null || firstBackoff.isZero() || firstBackoff.isNegative()) {
              throw new IllegalStateException(
                  "gateway.routes." + key + ".resilience.retry.backoff.first-backoff must be a positive duration");
            }
            if (maxBackoff == null || maxBackoff.isZero() || maxBackoff.isNegative()) {
              throw new IllegalStateException(
                  "gateway.routes." + key + ".resilience.retry.backoff.max-backoff must be a positive duration");
            }
            if (maxBackoff.compareTo(firstBackoff) < 0) {
              throw new IllegalStateException(
                  "gateway.routes." + key + ".resilience.retry.backoff.max-backoff must be greater than or equal to first-backoff");
            }
            if (factor < 1) {
              throw new IllegalStateException(
                  "gateway.routes." + key + ".resilience.retry.backoff.factor must be at least 1");
            }
          }

          @Override
          public boolean equals(Object o) {
            if (this == o) {
              return true;
            }
            if (!(o instanceof Backoff backoff)) {
              return false;
            }
            return enabled == backoff.enabled
                && factor == backoff.factor
                && basedOnPreviousValue == backoff.basedOnPreviousValue
                && Objects.equals(firstBackoff, backoff.firstBackoff)
                && Objects.equals(maxBackoff, backoff.maxBackoff);
          }

          @Override
          public int hashCode() {
            return Objects.hash(enabled, firstBackoff, maxBackoff, factor, basedOnPreviousValue);
          }

          @Override
          public String toString() {
            return "Backoff{"
                + "enabled=" + enabled
                + ", firstBackoff=" + firstBackoff
                + ", maxBackoff=" + maxBackoff
                + ", factor=" + factor
                + ", basedOnPreviousValue=" + basedOnPreviousValue
                + '}';
          }
        }
      }
    }
  }
}
