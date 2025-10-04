package com.ejada.gateway.transformation;

import com.ejada.gateway.config.GatewayTransformationProperties;
import com.ejada.gateway.config.GatewayTransformationProperties.HeaderOperations;
import com.ejada.gateway.config.GatewayTransformationProperties.HeaderRuleSet;
import com.ejada.gateway.config.GatewayTransformationProperties.HeaderValue;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

/**
 * Applies declarative header mutations to requests and responses.
 */
public class HeaderTransformationService {

  private final GatewayTransformationProperties properties;

  public HeaderTransformationService(GatewayTransformationProperties properties) {
    this.properties = Objects.requireNonNull(properties, "properties");
  }

  public HeaderOperations resolveRequestOperations(String routeId) {
    HeaderRuleSet ruleSet = properties.getHeaders().findRoute(routeId);
    return ruleSet != null ? ruleSet.resolveRequestOperations() : new HeaderOperations();
  }

  public HeaderOperations resolveResponseOperations(String routeId) {
    HeaderRuleSet ruleSet = properties.getHeaders().findRoute(routeId);
    return ruleSet != null ? ruleSet.resolveResponseOperations() : new HeaderOperations();
  }

  public void applyRequestHeaders(HttpHeaders headers, HeaderOperations operations) {
    apply(headers, operations);
  }

  public void applyResponseHeaders(HttpHeaders headers, HeaderOperations operations) {
    apply(headers, operations);
  }

  private void apply(HttpHeaders headers, HeaderOperations operations) {
    if (headers == null || operations == null || operations.isEmpty()) {
      return;
    }
    List<HeaderValue> add = operations.getAdd();
    for (HeaderValue value : add) {
      if (value == null || !StringUtils.hasText(value.getName())) {
        continue;
      }
      headers.add(value.getName(), value.getValue());
    }

    for (String remove : operations.getRemove()) {
      headers.remove(remove);
    }

    for (HeaderValue modify : operations.getModify()) {
      if (modify == null || !StringUtils.hasText(modify.getName())) {
        continue;
      }
      headers.set(modify.getName(), modify.getValue());
    }
  }
}

