package com.ejada.gateway.security;

import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.GatewaySecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Coordinates proactive JWT refresh operations for tokens that are nearing expiry.
 */
public class GatewayTokenRefreshService {

  private static final Logger LOGGER = LoggerFactory.getLogger(GatewayTokenRefreshService.class);
  private static final ParameterizedTypeReference<BaseResponse<TokenRefreshPayload>> RESPONSE_TYPE =
      new ParameterizedTypeReference<>() { };

  private final GatewaySecurityProperties properties;
  private final WebClient webClient;

  public GatewayTokenRefreshService(GatewaySecurityProperties properties,
      WebClient.Builder webClientBuilder,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> objectMapper) {
    this.properties = Objects.requireNonNull(properties, "properties");
    WebClient.Builder builder = Objects.requireNonNull(webClientBuilder, "webClientBuilder").clone();
    ObjectMapper mapper = (objectMapper != null) ? objectMapper.getIfAvailable() : null;
    if (mapper != null) {
      builder.codecs(configurer -> configurer.defaultCodecs().jackson2JsonEncoder(
          new org.springframework.http.codec.json.Jackson2JsonEncoder(mapper)));
      builder.codecs(configurer -> configurer.defaultCodecs().jackson2JsonDecoder(
          new org.springframework.http.codec.json.Jackson2JsonDecoder(mapper)));
    }
    this.webClient = builder.build();
  }

  public Mono<String> refreshIfNecessary(String tenantId, Jwt jwt, String rawToken) {
    GatewaySecurityProperties.TokenRefresh cfg = properties.getTokenRefresh();
    if (!cfg.isEnabled()) {
      return Mono.empty();
    }
    if (jwt == null || jwt.getExpiresAt() == null) {
      return Mono.empty();
    }
    Duration window = cfg.getRefreshWindow();
    if (window == null || window.isNegative() || window.isZero()) {
      window = Duration.ofMinutes(5);
    }
    Instant expiresAt = jwt.getExpiresAt();
    Instant threshold = Instant.now().plus(window);
    if (expiresAt.isAfter(threshold)) {
      return Mono.empty();
    }
    if (!StringUtils.hasText(rawToken)) {
      return Mono.empty();
    }
    return requestRefresh(tenantId, rawToken)
        .doOnError(ex -> LOGGER.warn("Failed to refresh JWT for tenant {}", tenantId, ex))
        .onErrorResume(ex -> Mono.empty());
  }

  private Mono<String> requestRefresh(String tenantId, String rawToken) {
    GatewaySecurityProperties.TokenRefresh cfg = properties.getTokenRefresh();
    String uri = cfg.resolveUri(tenantId);
    if (!StringUtils.hasText(uri)) {
      return Mono.empty();
    }
    WebClient.RequestBodySpec spec = webClient.post()
        .uri(uri)
        .contentType(MediaType.APPLICATION_JSON);
    if (cfg.isPropagateTenantHeader() && StringUtils.hasText(tenantId)) {
      spec = spec.header("X-Tenant-Id", tenantId);
    }
    spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + rawToken);
    return spec.bodyValue(new TokenRefreshRequest(rawToken))
        .retrieve()
        .bodyToMono(RESPONSE_TYPE)
        .map(BaseResponse::getData)
        .filter(Objects::nonNull)
        .map(TokenRefreshPayload::token)
        .filter(StringUtils::hasText);
  }

  private record TokenRefreshRequest(String token) { }

  private record TokenRefreshPayload(String token, Instant expiresAt) { }
}
