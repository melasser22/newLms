package com.ejada.gateway.filter;

import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.SubscriptionValidationProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.util.AntPathMatcher;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

/**
 * Validates tenant subscriptions before forwarding traffic to downstream
 * services. Results are cached in Redis to avoid overwhelming the
 * subscription-service and to minimise latency.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class SubscriptionValidationGatewayFilter implements GlobalFilter, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionValidationGatewayFilter.class);

  private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
  private static final ParameterizedTypeReference<BaseResponse<SubscriptionPayload>> RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {
      };

  private final SubscriptionValidationProperties properties;
  private final ReactiveStringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final WebClient webClient;

  public SubscriptionValidationGatewayFilter(
      SubscriptionValidationProperties properties,
      ReactiveStringRedisTemplate redisTemplate,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper,
      WebClient.Builder webClientBuilder) {
    this.properties = Objects.requireNonNull(properties, "properties");
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
    ObjectMapper mapper = (primaryObjectMapper != null) ? primaryObjectMapper.getIfAvailable() : null;
    if (mapper == null) {
      mapper = (fallbackObjectMapper != null) ? fallbackObjectMapper.getIfAvailable() : null;
    }
    this.objectMapper = Objects.requireNonNull(mapper, "objectMapper");

    this.webClient = webClientBuilder.clone().build();
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

    return ensureSubscription(tenantId, requiredFeature)
        .flatMap(decision -> {
          if (decision.allowed()) {
            exchange.getAttributes().put(GatewayRequestAttributes.SUBSCRIPTION, decision.record());
            String tier = decision.record() != null ? decision.record().tier() : null;
            if (StringUtils.hasText(tier)) {
              exchange.getAttributes().put(GatewayRequestAttributes.SUBSCRIPTION_TIER, tier);
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

  private Mono<SubscriptionDecision> ensureSubscription(String tenantId, @Nullable String requiredFeature) {
    String cacheKey = properties.cacheKey(tenantId, requiredFeature);
    return redisTemplate.opsForValue().get(cacheKey)
        .flatMap(json -> decode(json).map(Mono::just).orElseGet(Mono::empty))
        .switchIfEmpty(Mono.defer(() -> fetchSubscription(tenantId)
            .flatMap(record -> cache(cacheKey, record).thenReturn(record))))
        .map(record -> evaluate(record, requiredFeature))
        .onErrorResume(ex -> {
          LOGGER.warn("Subscription validation failed for tenant {}", tenantId, ex);
          if (properties.isFailOpen()) {
            return Mono.just(SubscriptionDecision.allow(null));
          }
          return Mono.just(SubscriptionDecision.deny(HttpStatus.SERVICE_UNAVAILABLE,
              "ERR_SUBSCRIPTION_UNAVAILABLE",
              "Unable to validate subscription status"));
        });
  }

  private Mono<Void> cache(String cacheKey, SubscriptionRecord record) {
    try {
      String value = objectMapper.writeValueAsString(record);
      Duration ttl = Optional.ofNullable(properties.getCacheTtl()).filter(d -> !d.isZero() && !d.isNegative())
          .orElse(Duration.ofMinutes(5));
      return redisTemplate.opsForValue().set(cacheKey, value, ttl).then();
    } catch (JsonProcessingException e) {
      LOGGER.debug("Failed to serialise subscription record for cache", e);
      return Mono.empty();
    }
  }

  private Optional<SubscriptionRecord> decode(String json) {
    try {
      return Optional.ofNullable(objectMapper.readValue(json, SubscriptionRecord.class));
    } catch (Exception ex) {
      LOGGER.debug("Failed to decode cached subscription payload", ex);
      return Optional.empty();
    }
  }

  private Mono<SubscriptionRecord> fetchSubscription(String tenantId) {
    return webClient.get()
        .uri(properties.getValidationUri(), tenantId)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(RESPONSE_TYPE)
        .map(this::extractPayload)
        .doOnNext(record -> LOGGER.debug("Fetched subscription for tenant {} -> {}", tenantId, record.status))
        .switchIfEmpty(Mono.just(SubscriptionRecord.inactive()));
  }

  private SubscriptionRecord extractPayload(BaseResponse<SubscriptionPayload> response) {
    if (response == null) {
      return SubscriptionRecord.inactive();
    }
    SubscriptionPayload payload = response.getData();
    if (payload == null) {
      return SubscriptionRecord.inactive();
    }
    boolean active = payload.active != null ? payload.active : "ACTIVE".equalsIgnoreCase(payload.status);
    Set<String> features = (payload.features != null)
        ? new HashSet<>(payload.features)
        : Collections.emptySet();
    return SubscriptionRecord.of(active, features, payload.expiresAt, payload.tier);
  }

  private SubscriptionDecision evaluate(SubscriptionRecord record, @Nullable String requiredFeature) {
    if (!record.isActive()) {
      return SubscriptionDecision.deny(HttpStatus.PAYMENT_REQUIRED,
          "ERR_SUBSCRIPTION_INACTIVE",
          "Subscription is inactive or expired");
    }
    if (StringUtils.hasText(requiredFeature) && !record.hasFeature(requiredFeature)) {
      return SubscriptionDecision.deny(HttpStatus.FORBIDDEN,
          "ERR_SUBSCRIPTION_FEATURE", "Subscription does not include required feature");
    }
    return SubscriptionDecision.allow(record);
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

    static SubscriptionDecision allow(SubscriptionRecord record) {
      return new SubscriptionDecision(true, record, HttpStatus.OK, null, null);
    }

    static SubscriptionDecision deny(HttpStatus status, String code, String message) {
      return new SubscriptionDecision(false, null, status, code, message);
    }
  }

  private record SubscriptionRecord(boolean active, Set<String> features, Instant expiresAt, String status,
      String tier) {

    SubscriptionRecord {
      features = (features == null) ? Set.of() : Set.copyOf(features);
      status = StringUtils.hasText(status) ? status : (active ? "ACTIVE" : "INACTIVE");
      tier = (tier == null) ? null : tier.trim();
    }

    static SubscriptionRecord of(boolean active, Set<String> features, Instant expiresAt, @Nullable String tier) {
      return new SubscriptionRecord(active, features, expiresAt, active ? "ACTIVE" : "INACTIVE", tier);
    }

    static SubscriptionRecord inactive() {
      return new SubscriptionRecord(false, Set.of(), null, "INACTIVE", null);
    }

    boolean isActive() {
      if (!active) {
        return false;
      }
      if (expiresAt == null) {
        return true;
      }
      return expiresAt.isAfter(Instant.now());
    }

    boolean hasFeature(String feature) {
      if (!StringUtils.hasText(feature)) {
        return true;
      }
      return features.stream().anyMatch(value -> value.equalsIgnoreCase(feature));
    }
  }

  private static final class SubscriptionPayload {
    private Boolean active;
    private String status;
    private java.util.List<String> features;
    private Instant expiresAt;
    private String tier;

    SubscriptionPayload() {
    }
  }
}

