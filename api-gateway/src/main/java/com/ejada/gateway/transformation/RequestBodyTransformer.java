package com.ejada.gateway.transformation;

import com.ejada.gateway.metrics.GatewayMetrics;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies compiled request transformation rules to JSON payloads.
 */
public class RequestBodyTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestBodyTransformer.class);

  private final TransformationRuleCache ruleCache;

  private final GatewayMetrics metrics;

  public RequestBodyTransformer(TransformationRuleCache ruleCache, GatewayMetrics metrics) {
    this.ruleCache = Objects.requireNonNull(ruleCache, "ruleCache");
    this.metrics = Objects.requireNonNull(metrics, "metrics");
  }

  public byte[] transform(String routeId, byte[] original) {
    List<CompiledRequestRule> rules = ruleCache.resolveRequestRules(routeId);
    if (rules.isEmpty() || original == null || original.length == 0) {
      return original;
    }

    try {
      DocumentContext context = JsonPath.using(BASE_CONFIGURATION)
          .parse(new String(original, StandardCharsets.UTF_8));
      boolean mutated = false;
      for (CompiledRequestRule rule : rules) {
        if (rule.apply(context)) {
          mutated = true;
        }
      }
      if (!mutated) {
        return original;
      }
      metrics.recordRequestTransformation();
      return context.jsonString().getBytes(StandardCharsets.UTF_8);
    } catch (Exception ex) {
      LOGGER.warn("Failed to apply request transformations for route {}", routeId, ex);
      return original;
    }
  }
}

  private static final Configuration BASE_CONFIGURATION = Configuration.defaultConfiguration()
      .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS, Option.CREATE_PATH);

