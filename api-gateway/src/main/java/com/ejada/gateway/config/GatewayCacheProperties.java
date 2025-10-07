package com.ejada.gateway.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Configuration properties for gateway response caching and warmup.
 */
@ConfigurationProperties(prefix = "gateway.cache")
public class GatewayCacheProperties {

  private static final PathPatternParser PATH_PATTERN_PARSER = new PathPatternParser();

  private boolean enabled;

  private Duration warmInterval = Duration.ofMinutes(15);

  private List<String> warmTenants = new ArrayList<>();

  private final List<RouteCacheProperties> routes = new ArrayList<>();

  private final Topics topics = new Topics();

  private final Kafka kafka = new Kafka();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Duration getWarmInterval() {
    return warmInterval;
  }

  public void setWarmInterval(Duration warmInterval) {
    if (warmInterval == null || warmInterval.isNegative()) {
      this.warmInterval = Duration.ofMinutes(15);
      return;
    }
    this.warmInterval = warmInterval;
  }

  public List<String> getWarmTenants() {
    return Collections.unmodifiableList(warmTenants);
  }

  public void setWarmTenants(List<String> warmTenants) {
    this.warmTenants.clear();
    if (warmTenants == null) {
      return;
    }
    warmTenants.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .map(value -> value.toLowerCase(Locale.ROOT))
        .forEach(this.warmTenants::add);
  }

  public List<RouteCacheProperties> getRoutes() {
    return Collections.unmodifiableList(routes);
  }

  public void setRoutes(List<RouteCacheProperties> routeConfigs) {
    this.routes.clear();
    if (routeConfigs == null) {
      return;
    }
    routeConfigs.stream()
        .filter(Objects::nonNull)
        .forEach(route -> this.routes.add(route.initialise()));
  }

  public Optional<RouteCacheProperties> getRouteById(String id) {
    if (!StringUtils.hasText(id)) {
      return Optional.empty();
    }
    return routes.stream()
        .filter(route -> id.equals(route.getId()))
        .findFirst();
  }

  public Optional<RouteCacheProperties> resolve(String routeId, ServerWebExchange exchange) {
    if (!enabled) {
      return Optional.empty();
    }
    return routes.stream()
        .filter(route -> route.matches(routeId, exchange))
        .findFirst();
  }

  public Topics getTopics() {
    return topics;
  }

  public Kafka getKafka() {
    return kafka;
  }

  public static class Topics {

    private String tenantUpdated = "tenant.updated";

    private String catalogPlanUpdated = "catalog.plan.updated";

    public String getTenantUpdated() {
      return tenantUpdated;
    }

    public void setTenantUpdated(String tenantUpdated) {
      if (StringUtils.hasText(tenantUpdated)) {
        this.tenantUpdated = tenantUpdated.trim();
      }
    }

    public String getCatalogPlanUpdated() {
      return catalogPlanUpdated;
    }

    public void setCatalogPlanUpdated(String catalogPlanUpdated) {
      if (StringUtils.hasText(catalogPlanUpdated)) {
        this.catalogPlanUpdated = catalogPlanUpdated.trim();
      }
    }
  }

  public static class Kafka {

    private String groupId = "gateway-cache";

    private boolean enabled = true;

    public String getGroupId() {
      return groupId;
    }

    public void setGroupId(String groupId) {
      if (StringUtils.hasText(groupId)) {
        this.groupId = groupId.trim();
      }
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  public static class RouteCacheProperties {

    private String id;

    private String routeId;

    private HttpMethod method = HttpMethod.GET;

    private String path;

    private Duration ttl = Duration.ZERO;

    private Duration staleTtl;

    private boolean warm;

    private boolean tenantScoped = true;

    private String warmPath;

    private final ClientProperties client = new ClientProperties();

    private transient PathPattern pathPattern;

    RouteCacheProperties initialise() {
      if (StringUtils.hasText(path)) {
        this.pathPattern = PATH_PATTERN_PARSER.parse(path.trim());
      }
      return this;
    }

    public boolean matches(String candidateRouteId, ServerWebExchange exchange) {
      if (!StringUtils.hasText(path) || pathPattern == null) {
        return false;
      }
      if (method != null && exchange.getRequest().getMethod() != method) {
        return false;
      }
      if (StringUtils.hasText(routeId) && !Objects.equals(routeId, candidateRouteId)) {
        return false;
      }
      ServerHttpRequest request = exchange.getRequest();
      return pathPattern.matches(request.getPath().pathWithinApplication());
    }

    public String cacheKeyPrefix() {
      if (StringUtils.hasText(id)) {
        return id;
      }
      if (StringUtils.hasText(routeId)) {
        return routeId;
      }
      return pathPattern != null ? pathPattern.getPatternString() : "unknown";
    }

    public Duration resolvedTtl() {
      if (ttl == null || ttl.isNegative() || ttl.isZero()) {
        return Duration.ZERO;
      }
      return ttl;
    }

    public Duration resolvedStaleTtl() {
      if (staleTtl == null) {
        Duration base = resolvedTtl();
        if (base.isZero()) {
          return Duration.ZERO;
        }
        return base.dividedBy(2);
      }
      if (staleTtl.isNegative()) {
        return Duration.ZERO;
      }
      return staleTtl;
    }

    public String normalisedWarmPath() {
      if (StringUtils.hasText(warmPath)) {
        return warmPath;
      }
      return path;
    }

    public boolean hasPathVariable() {
      return pathPattern != null && pathPattern.getPatternString().contains("{");
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = StringUtils.hasText(id) ? id.trim() : null;
    }

    public String getRouteId() {
      return routeId;
    }

    public void setRouteId(String routeId) {
      this.routeId = StringUtils.hasText(routeId) ? routeId.trim() : null;
    }

    public HttpMethod getMethod() {
      return method;
    }

    public void setMethod(HttpMethod method) {
      this.method = method;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
      if (StringUtils.hasText(path)) {
        this.pathPattern = PATH_PATTERN_PARSER.parse(path.trim());
      }
    }

    public Duration getTtl() {
      return ttl;
    }

    public void setTtl(Duration ttl) {
      this.ttl = ttl;
    }

    public Duration getStaleTtl() {
      return staleTtl;
    }

    public void setStaleTtl(Duration staleTtl) {
      this.staleTtl = staleTtl;
    }

    public boolean isWarm() {
      return warm;
    }

    public void setWarm(boolean warm) {
      this.warm = warm;
    }

    public boolean isTenantScoped() {
      return tenantScoped;
    }

    public void setTenantScoped(boolean tenantScoped) {
      this.tenantScoped = tenantScoped;
    }

    public String getWarmPath() {
      return warmPath;
    }

    public void setWarmPath(String warmPath) {
      this.warmPath = StringUtils.hasText(warmPath) ? warmPath.trim() : null;
    }

    public ClientProperties getClient() {
      return client;
    }

    public PathPattern getPathPattern() {
      return pathPattern;
    }

    @Override
    public String toString() {
      return "RouteCacheProperties{" +
          "id='" + id + '\'' +
          ", routeId='" + routeId + '\'' +
          ", method=" + method +
          ", path='" + path + '\'' +
          ", ttl=" + ttl +
          ", staleTtl=" + staleTtl +
          ", warm=" + warm +
          ", tenantScoped=" + tenantScoped +
          '}';
    }
  }

  public static class ClientProperties {

    private String authorization;

    private String apiKey;

    private Duration timeout = Duration.ofSeconds(10);

    private final Map<String, String> headers = new LinkedHashMap<>();

    public String getAuthorization() {
      return authorization;
    }

    public void setAuthorization(String authorization) {
      this.authorization = StringUtils.hasText(authorization) ? authorization.trim() : null;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = StringUtils.hasText(apiKey) ? apiKey.trim() : null;
    }

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      if (timeout == null || timeout.isZero() || timeout.isNegative()) {
        this.timeout = Duration.ofSeconds(10);
        return;
      }
      this.timeout = timeout;
    }

    public Map<String, String> getHeaders() {
      return Collections.unmodifiableMap(headers);
    }

    public void setHeaders(Map<String, String> headers) {
      this.headers.clear();
      if (headers == null || headers.isEmpty()) {
        return;
      }
      headers.forEach((key, value) -> {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(value)) {
          return;
        }
        this.headers.put(key.trim(), value.trim());
      });
    }

    public Duration resolvedTimeout() {
      if (timeout == null || timeout.isZero() || timeout.isNegative()) {
        return Duration.ofSeconds(10);
      }
      return timeout;
    }
  }

  public String describeRoutes() {
    return routes.stream().map(RouteCacheProperties::toString).collect(Collectors.joining(","));
  }
}
