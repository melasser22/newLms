package com.ejada.gateway.context;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.dto.BaseResponse;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import com.ejada.starter_core.web.FilterSkipUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Resolves the tenant identifier early in the gateway pipeline so other
 * filters (rate limiting, subscription validation) can rely on a canonical
 * value. The logic mirrors the servlet {@code ContextFilter} semantics by
 * checking header, query parameter, JWT claims, and finally the request host
 * (subdomain pattern).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class TenantExtractionGatewayFilter implements WebFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(TenantExtractionGatewayFilter.class);

  private static final Set<String> IGNORED_SUBDOMAINS = Set.of("www", "api", "edge");

  private final CoreAutoConfiguration.CoreProps props;
  private final ObjectMapper objectMapper;

  public TenantExtractionGatewayFilter(CoreAutoConfiguration.CoreProps props, ObjectMapper objectMapper) {
    this.props = Objects.requireNonNull(props, "props");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    var tenantProps = props.getTenant();
    if (!tenantProps.isEnabled()) {
      return chain.filter(exchange);
    }

    String path = exchange.getRequest().getPath().pathWithinApplication().value();
    if (FilterSkipUtils.shouldSkip(path, tenantProps.getSkipPatterns())) {
      return chain.filter(exchange);
    }

    Mono<Optional<Authentication>> authentication = exchange.getPrincipal()
        .filter(Authentication.class::isInstance)
        .cast(Authentication.class)
        .map(Optional::of)
        .defaultIfEmpty(Optional.empty())
        .cache(Duration.ofSeconds(1));

    return authentication.flatMap(optionalAuth -> {
      String fromHeader = trimToNull(exchange.getRequest().getHeaders().getFirst(tenantProps.getHeaderName()));
      String fromQuery = trimToNull(exchange.getRequest().getQueryParams().getFirst(tenantProps.getQueryParam()));
      String fromJwt = optionalAuth.map(auth -> extractFromJwt(auth, tenantProps.getJwtClaimNames())).orElse(null);
      String fromHost = extractFromHost(exchange.getRequest());

      String candidate = tenantProps.isPreferHeaderOverJwt()
          ? firstNonNull(fromHeader, fromQuery, fromJwt, fromHost)
          : firstNonNull(fromJwt, fromHeader, fromQuery, fromHost);

      if (!StringUtils.hasText(candidate)) {
        return chain.filter(exchange);
      }

      String sanitized = sanitize(candidate);
      if (sanitized == null) {
        return reject(exchange, candidate);
      }

      ServerWebExchange mutated = mutateRequest(exchange, tenantProps.getHeaderName(), sanitized, fromHeader);
      mutated.getAttributes().put(GatewayRequestAttributes.TENANT_ID, sanitized);
      mutated.getAttributes().putIfAbsent(HeaderNames.X_TENANT_ID, sanitized);
      return chain.filter(mutated)
          .contextWrite(context -> {
            Context updated = context
                .put(GatewayRequestAttributes.TENANT_ID, sanitized)
                .put(HeaderNames.X_TENANT_ID, sanitized);
            String correlationId = mutated.getAttribute(GatewayRequestAttributes.CORRELATION_ID);
            if (StringUtils.hasText(correlationId)) {
              updated = updated
                  .put(GatewayRequestAttributes.CORRELATION_ID, correlationId)
                  .put(HeaderNames.CORRELATION_ID, correlationId);
            }
            return updated;
          });
    });
  }

  private ServerWebExchange mutateRequest(ServerWebExchange exchange, String headerName, String tenant, @Nullable String originalHeader) {
    if (StringUtils.hasText(originalHeader)) {
      return exchange;
    }
    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
        .header(headerName, tenant)
        .build();
    return exchange.mutate().request(mutatedRequest).build();
  }

  @Nullable
  private String extractFromJwt(Authentication authentication, String[] claimNames) {
    if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
      return null;
    }
    for (String claim : claimNames) {
      Object value = jwtAuthenticationToken.getToken().getClaims().get(claim);
      if (value != null) {
        String tenant = trimToNull(Objects.toString(value, null));
        if (StringUtils.hasText(tenant)) {
          return tenant;
        }
      }
    }
    return null;
  }

  @Nullable
  private String extractFromHost(ServerHttpRequest request) {
    String hostHeader = request.getHeaders().getFirst("Host");
    if (!StringUtils.hasText(hostHeader)) {
      hostHeader = request.getURI().getHost();
    }
    if (!StringUtils.hasText(hostHeader)) {
      return null;
    }
    String host = hostHeader.toLowerCase(Locale.ROOT);
    if (host.chars().allMatch(Character::isDigit)) {
      return null;
    }
    String[] parts = host.split("\\.");
    if (parts.length < 2) {
      return null;
    }
    String first = parts[0];
    if (!StringUtils.hasText(first) || IGNORED_SUBDOMAINS.contains(first)) {
      return null;
    }
    return first;
  }

  @Nullable
  private String sanitize(String candidate) {
    if (!StringUtils.hasText(candidate)) {
      return null;
    }
    String trimmed = candidate.trim();
    if (!trimmed.matches("[A-Za-z0-9_-]{1,36}")) {
      return null;
    }
    return trimmed;
  }

  private Mono<Void> reject(ServerWebExchange exchange, String rawValue) {
    LOGGER.warn("Rejecting request due to invalid tenant identifier: {}", rawValue);
    var response = exchange.getResponse();
    response.setStatusCode(HttpStatus.BAD_REQUEST);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    BaseResponse<Void> body = BaseResponse.error("ERR_INVALID_TENANT", "Invalid " + HeaderNames.X_TENANT_ID);
    byte[] payload;
    try {
      payload = objectMapper.writeValueAsBytes(body);
    } catch (JsonProcessingException e) {
      payload = body.toString().getBytes(StandardCharsets.UTF_8);
    }
    return response.writeWith(Mono.just(response.bufferFactory().wrap(payload)));
  }

  private static String firstNonNull(String... values) {
    return Arrays.stream(values)
        .filter(StringUtils::hasText)
        .map(String::trim)
        .findFirst()
        .orElse(null);
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}

