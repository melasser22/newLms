package com.ejada.gateway.filter;

import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.SubscriptionValidationProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.subscription.SubscriptionRecord;
import com.ejada.gateway.subscription.SubscriptionValidationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Validates tenant subscriptions before forwarding traffic to downstream services.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class SubscriptionValidationGatewayFilter implements GlobalFilter, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionValidationGatewayFilter.class);
  private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

  private final SubscriptionValidationProperties properties;
  private final SubscriptionValidationService validationService;
  private final ObjectMapper objectMapper;

  public SubscriptionValidationGatewayFilter(
      SubscriptionValidationProperties properties,
      SubscriptionValidationService validationService,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper) {
    this.properties = Objects.requireNonNull(properties, "properties");
    this.validationService = Objects.requireNonNull(validationService, "validationService");
    ObjectMapper mapper = (primaryObjectMapper != null) ? primaryObjectMapper.getIfAvailable() : null;
    if (mapper == null) {
      mapper = (fallbackObjectMapper != null) ? fallbackObjectMapper.getIfAvailable() : null;
    }
    this.objectMapper = Objects.requireNonNull(mapper, "objectMapper");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!properties.isEnabled()) {
      return chain.filter(exchange);
    }

    String path = exchange.getRequest().getPath().value();
    if (shouldSkip(path)) {
      return chain.filter(exchange);
    }

    String tenantId = exchange.getAttribute(GatewayRequestAttributes.TENANT_ID);
    if (!StringUtils.hasText(tenantId)) {
      return chain.filter(exchange);
    }

    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    String routeId = route != null ? route.getId() : null;
    if (!properties.requiresValidation(routeId)) {
      return chain.filter(exchange);
    }

    String requiredFeature = properties.getRequiredFeatures().get(routeId);
    return validationService.getSubscription(tenantId, requiredFeature)
        .map(record -> evaluate(record, requiredFeature))
        .onErrorResume(ex -> {
          LOGGER.warn("Subscription validation failed for tenant {}", tenantId, ex);
          if (properties.isFailOpen()) {
            return Mono.just(SubscriptionDecision.allow(null));
          }
          return Mono.just(SubscriptionDecision.deny(HttpStatus.SERVICE_UNAVAILABLE,
              "ERR_SUBSCRIPTION_UNAVAILABLE", "Unable to validate subscription status"));
        })
        .flatMap(decision -> {
          if (decision.allowed()) {
            if (decision.record() != null) {
              exchange.getAttributes().put(GatewayRequestAttributes.SUBSCRIPTION, decision.record());
            }
            return chain.filter(exchange);
          }
          return reject(exchange, decision.status(), decision.code(), decision.message());
        });
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 40;
  }

  private boolean shouldSkip(String path) {
    for (String pattern : properties.getSkipPatterns()) {
      if (StringUtils.hasText(pattern) && ANT_PATH_MATCHER.match(pattern, path)) {
        return true;
      }
    }
    return false;
  }

  private SubscriptionDecision evaluate(@Nullable SubscriptionRecord record, @Nullable String requiredFeature) {
    SubscriptionRecord candidate = (record != null) ? record : SubscriptionRecord.inactive();
    if (!candidate.isActive()) {
      return SubscriptionDecision.deny(HttpStatus.PAYMENT_REQUIRED,
          "ERR_SUBSCRIPTION_INACTIVE", "Subscription is inactive or expired");
    }
    if (StringUtils.hasText(requiredFeature) && !candidate.hasFeature(requiredFeature)) {
      return SubscriptionDecision.deny(HttpStatus.FORBIDDEN,
          "ERR_SUBSCRIPTION_FEATURE", "Subscription does not include required feature");
    }
    return SubscriptionDecision.allow(candidate);
  }

  private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String code, String message) {
    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    BaseResponse<Void> body = BaseResponse.error(code, message);
    byte[] payload;
    try {
      payload = objectMapper.writeValueAsBytes(body);
    } catch (JsonProcessingException e) {
      payload = body.toString().getBytes(StandardCharsets.UTF_8);
    }
    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
  }

  private record SubscriptionDecision(boolean allowed, SubscriptionRecord record, HttpStatus status, String code, String message) {

    static SubscriptionDecision allow(@Nullable SubscriptionRecord record) {
      return new SubscriptionDecision(true, record, HttpStatus.OK, null, null);
    }

    static SubscriptionDecision deny(HttpStatus status, String code, String message) {
      return new SubscriptionDecision(false, null, status, code, message);
    }
  }
}
