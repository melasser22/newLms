package com.ejada.gateway.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for request/response transformations and header manipulation.
 */
@ConfigurationProperties(prefix = "gateway.transformation")
public class GatewayTransformationProperties {

  private final RequestTransformations request = new RequestTransformations();

  private final ResponseTransformations response = new ResponseTransformations();

  private final HeaderTransformations headers = new HeaderTransformations();

  public RequestTransformations getRequest() {
    return request;
  }

  public ResponseTransformations getResponse() {
    return response;
  }

  public HeaderTransformations getHeaders() {
    return headers;
  }

  /**
   * Holder for request transformation rules keyed by route identifier.
   */
  public static class RequestTransformations {

    private final AtomicLong version = new AtomicLong();

    private Map<String, RouteTransformation> routes = new LinkedHashMap<>();

    public Map<String, RouteTransformation> getRoutes() {
      return routes;
    }

    public void setRoutes(Map<String, RouteTransformation> routes) {
      LinkedHashMap<String, RouteTransformation> copy = new LinkedHashMap<>();
      if (routes != null) {
        routes.forEach((key, value) -> {
          if (!StringUtils.hasText(key) || value == null) {
            return;
          }
          copy.put(key.trim(), value);
        });
      }
      this.routes = copy;
      version.incrementAndGet();
    }

    public RouteTransformation findRoute(String routeId) {
      if (!StringUtils.hasText(routeId)) {
        return null;
      }
      return routes.get(routeId);
    }

    public long getVersion() {
      return version.get();
    }
  }

  /**
   * Indicates whether response wrapping is enabled.
   */
  public static class ResponseTransformations {

    private boolean wrapEnabled = false;

    private String version = "unknown";

    public boolean isWrapEnabled() {
      return wrapEnabled;
    }

    public void setWrapEnabled(boolean wrapEnabled) {
      this.wrapEnabled = wrapEnabled;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      if (!StringUtils.hasText(version)) {
        this.version = "unknown";
        return;
      }
      this.version = version.trim();
    }
  }

  /**
   * Header transformation rules for requests and responses.
   */
  public static class HeaderTransformations {

    private Map<String, HeaderRuleSet> routes = new LinkedHashMap<>();

    public Map<String, HeaderRuleSet> getRoutes() {
      return routes;
    }

    public void setRoutes(Map<String, HeaderRuleSet> routes) {
      LinkedHashMap<String, HeaderRuleSet> copy = new LinkedHashMap<>();
      if (routes != null) {
        routes.forEach((key, value) -> {
          if (!StringUtils.hasText(key) || value == null) {
            return;
          }
          copy.put(key.trim(), value);
        });
      }
      this.routes = copy;
    }

    public HeaderRuleSet findRoute(String routeId) {
      if (!StringUtils.hasText(routeId)) {
        return null;
      }
      return routes.get(routeId);
    }
  }

  /**
   * Group of header operations. Supports global rules and request/response specific overrides.
   */
  public static class HeaderRuleSet {

    private HeaderOperations global = new HeaderOperations();

    private HeaderOperations request = new HeaderOperations();

    private HeaderOperations response = new HeaderOperations();

    public HeaderOperations getGlobal() {
      return global;
    }

    public void setGlobal(HeaderOperations global) {
      this.global = (global == null) ? new HeaderOperations() : global;
    }

    public HeaderOperations getRequest() {
      return request;
    }

    public void setRequest(HeaderOperations request) {
      this.request = (request == null) ? new HeaderOperations() : request;
    }

    public HeaderOperations getResponse() {
      return response;
    }

    public void setResponse(HeaderOperations response) {
      this.response = (response == null) ? new HeaderOperations() : response;
    }

    public List<HeaderValue> getAdd() {
      return global.getAdd();
    }

    public void setAdd(List<HeaderValue> add) {
      global.setAdd(add);
    }

    public List<String> getRemove() {
      return global.getRemove();
    }

    public void setRemove(List<String> remove) {
      global.setRemove(remove);
    }

    public List<HeaderValue> getModify() {
      return global.getModify();
    }

    public void setModify(List<HeaderValue> modify) {
      global.setModify(modify);
    }

    public HeaderOperations resolveRequestOperations() {
      return HeaderOperations.combine(global, request);
    }

    public HeaderOperations resolveResponseOperations() {
      return HeaderOperations.combine(global, response);
    }
  }

  /**
   * Describes a request transformation rule for JSON payloads.
   */
  public static class RouteTransformation {

    private List<RequestRule> rules = new ArrayList<>();

    public List<RequestRule> getRules() {
      return rules;
    }

    public void setRules(List<RequestRule> rules) {
      if (CollectionUtils.isEmpty(rules)) {
        this.rules = new ArrayList<>();
        return;
      }
      List<RequestRule> copy = new ArrayList<>();
      for (RequestRule rule : rules) {
        if (rule != null && StringUtils.hasText(rule.getJsonPath())) {
          copy.add(rule);
        }
      }
      this.rules = copy;
    }
  }

  /**
   * Rule definition referencing a JSONPath expression and an operation.
   */
  public static class RequestRule {

    private String jsonPath;

    private RequestOperation operation = RequestOperation.SET;

    private Object value;

    private String targetPath;

    public String getJsonPath() {
      return jsonPath;
    }

    public void setJsonPath(String jsonPath) {
      if (!StringUtils.hasText(jsonPath)) {
        this.jsonPath = null;
        return;
      }
      this.jsonPath = jsonPath.trim();
    }

    public RequestOperation getOperation() {
      return operation;
    }

    public void setOperation(RequestOperation operation) {
      this.operation = (operation == null) ? RequestOperation.SET : operation;
    }

    public void setOperation(String operation) {
      if (!StringUtils.hasText(operation)) {
        this.operation = RequestOperation.SET;
        return;
      }
      try {
        this.operation = RequestOperation.valueOf(operation.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ex) {
        this.operation = RequestOperation.SET;
      }
    }

    public Object getValue() {
      return value;
    }

    public void setValue(Object value) {
      this.value = value;
    }

    public String getTargetPath() {
      return targetPath;
    }

    public void setTargetPath(String targetPath) {
      this.targetPath = StringUtils.hasText(targetPath) ? targetPath.trim() : null;
    }

  }

  /**
   * Supported request transformation operations.
   */
  public enum RequestOperation {
    REMOVE,
    SET,
    ADD_IF_MISSING,
    RENAME
  }

  /**
   * Header operations containing add/remove/modify lists.
   */
  public static class HeaderOperations {

    private List<HeaderValue> add = new ArrayList<>();

    private List<String> remove = new ArrayList<>();

    private List<HeaderValue> modify = new ArrayList<>();

    public List<HeaderValue> getAdd() {
      return add;
    }

    public void setAdd(List<HeaderValue> add) {
      this.add = sanitizeHeaderValues(add);
    }

    public List<String> getRemove() {
      return remove;
    }

    public void setRemove(List<String> remove) {
      if (CollectionUtils.isEmpty(remove)) {
        this.remove = new ArrayList<>();
        return;
      }
      List<String> cleaned = new ArrayList<>();
      for (String entry : remove) {
        if (StringUtils.hasText(entry)) {
          cleaned.add(entry.trim());
        }
      }
      this.remove = cleaned;
    }

    public List<HeaderValue> getModify() {
      return modify;
    }

    public void setModify(List<HeaderValue> modify) {
      this.modify = sanitizeHeaderValues(modify);
    }

    public boolean isEmpty() {
      return add.isEmpty() && remove.isEmpty() && modify.isEmpty();
    }

    public static HeaderOperations combine(HeaderOperations first, HeaderOperations second) {
      HeaderOperations combined = new HeaderOperations();
      if (first != null) {
        combined.add.addAll(first.getAdd());
        combined.remove.addAll(first.getRemove());
        combined.modify.addAll(first.getModify());
      }
      if (second != null) {
        combined.add.addAll(second.getAdd());
        combined.remove.addAll(second.getRemove());
        combined.modify.addAll(second.getModify());
      }
      return combined;
    }

    private List<HeaderValue> sanitizeHeaderValues(List<HeaderValue> values) {
      if (CollectionUtils.isEmpty(values)) {
        return new ArrayList<>();
      }
      List<HeaderValue> cleaned = new ArrayList<>();
      for (HeaderValue value : values) {
        if (value == null) {
          continue;
        }
        String name = value.getName();
        if (!StringUtils.hasText(name)) {
          continue;
        }
        cleaned.add(value);
      }
      return cleaned;
    }
  }

  /**
   * Simple header name/value pair.
   */
  public static class HeaderValue {

    private String name;

    private String value;

    public HeaderValue() {
    }

    public HeaderValue(String name, String value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = StringUtils.hasText(name) ? name.trim() : null;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof HeaderValue that)) {
        return false;
      }
      return Objects.equals(name, that.name) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, value);
    }
  }
}

