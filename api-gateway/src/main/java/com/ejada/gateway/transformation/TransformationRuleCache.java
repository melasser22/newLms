package com.ejada.gateway.transformation;

import com.ejada.gateway.config.GatewayTransformationProperties;
import com.ejada.gateway.config.GatewayTransformationProperties.RequestTransformations;
import com.ejada.gateway.config.GatewayTransformationProperties.RouteTransformation;
import com.ejada.gateway.config.GatewayTransformationProperties.RequestRule;
import com.ejada.gateway.config.GatewayTransformationProperties.RequestOperation;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains a cached view of compiled JSONPath expressions for request transformation rules.
 */
public class TransformationRuleCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransformationRuleCache.class);

  private final GatewayTransformationProperties properties;

  private final ConcurrentMap<String, CachedRequestRules> requestRuleCache = new ConcurrentHashMap<>();

  private volatile long requestVersion = -1L;

  public TransformationRuleCache(GatewayTransformationProperties properties) {
    this.properties = Objects.requireNonNull(properties, "properties");
  }

  public List<CompiledRequestRule> resolveRequestRules(String routeId) {
    if (routeId == null) {
      return Collections.emptyList();
    }

    RequestTransformations request = properties.getRequest();
    long version = request.getVersion();

    if (version != requestVersion) {
      requestRuleCache.clear();
      requestVersion = version;
    }

    CachedRequestRules cached = requestRuleCache.compute(routeId, (key, existing) -> {
      if (existing != null && existing.version == version) {
        return existing;
      }
      RouteTransformation routeTransformation = request.findRoute(routeId);
      if (routeTransformation == null || routeTransformation.getRules().isEmpty()) {
        return new CachedRequestRules(version, Collections.emptyList());
      }
      List<CompiledRequestRule> compiled = routeTransformation.getRules().stream()
          .map(this::compileRule)
          .filter(Objects::nonNull)
          .toList();
      return new CachedRequestRules(version, compiled);
    });

    return cached.rules;
  }

  private CompiledRequestRule compileRule(RequestRule rule) {
    try {
      JsonPath path = JsonPath.using(BASE_CONFIGURATION).compile(rule.getJsonPath());
      JsonPath target = null;
      if (rule.getOperation() == RequestOperation.RENAME && rule.getTargetPath() != null) {
        target = JsonPath.using(BASE_CONFIGURATION).compile(rule.getTargetPath());
      }
      return new CompiledRequestRule(rule, path, target);
    } catch (Exception ex) {
      LOGGER.warn("Failed to compile JSONPath expression {}", rule.getJsonPath(), ex);
      return null;
    }
  }

  private record CachedRequestRules(long version, List<CompiledRequestRule> rules) {
  }
}

  private static final Configuration BASE_CONFIGURATION = Configuration.defaultConfiguration()
      .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS, Option.CREATE_PATH);

