package com.ejada.gateway.transformation;

import com.ejada.gateway.config.GatewayTransformationProperties.RequestOperation;
import com.ejada.gateway.config.GatewayTransformationProperties.RequestRule;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

/**
 * Value object bundling a compiled JSONPath and its transformation semantics.
 */
public class CompiledRequestRule {

  private final RequestRule rule;

  private final JsonPath path;

  private final JsonPath target;

  public CompiledRequestRule(RequestRule rule, JsonPath path, JsonPath target) {
    this.rule = rule;
    this.path = path;
    this.target = target;
  }

  public RequestRule getRule() {
    return rule;
  }

  public boolean apply(DocumentContext context) {
    if (context == null) {
      return false;
    }
    RequestOperation operation = rule.getOperation();
    return switch (operation) {
      case REMOVE -> remove(context);
      case RENAME -> rename(context);
      case ADD_IF_MISSING -> addIfMissing(context);
      case SET -> set(context);
    };
  }

  private boolean remove(DocumentContext context) {
    context.delete(path);
    return true;
  }

  private boolean rename(DocumentContext context) {
    if (target == null) {
      return false;
    }
    Object value = context.read(path);
    if (value == null) {
      return false;
    }
    context.delete(path);
    context.set(target, value);
    return true;
  }

  private boolean addIfMissing(DocumentContext context) {
    Object current = context.read(path);
    if (current != null) {
      return false;
    }
    context.set(path, rule.getValue());
    return true;
  }

  private boolean set(DocumentContext context) {
    context.set(path, rule.getValue());
    return true;
  }
}

