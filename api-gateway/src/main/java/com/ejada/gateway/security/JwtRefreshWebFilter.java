package com.ejada.gateway.security;

import com.ejada.gateway.config.GatewaySecurityProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive filter that refreshes JWT tokens nearing expiry and surfaces new tokens via response headers.
 */
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class JwtRefreshWebFilter implements WebFilter, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtRefreshWebFilter.class);
  private static final String[] TENANT_CLAIMS = {"tenant", "tenant_id", "tenantId", "tid"};

  private final GatewaySecurityProperties properties;
  private final GatewayTokenRefreshService tokenRefreshService;

  public JwtRefreshWebFilter(GatewaySecurityProperties properties,
      GatewayTokenRefreshService tokenRefreshService) {
    this.properties = Objects.requireNonNull(properties, "properties");
    this.tokenRefreshService = Objects.requireNonNull(tokenRefreshService, "tokenRefreshService");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    GatewaySecurityProperties.TokenRefresh cfg = properties.getTokenRefresh();
    if (!cfg.isEnabled()) {
      return chain.filter(exchange);
    }
    String rawToken = resolveBearer(exchange.getRequest().getHeaders());
    if (!StringUtils.hasText(rawToken)) {
      return chain.filter(exchange);
    }
    exchange.getResponse().beforeCommit(() -> ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .flatMap(auth -> attemptRefresh(auth, rawToken, exchange))
        .onErrorResume(ex -> {
          LOGGER.debug("JWT refresh attempt failed", ex);
          return Mono.empty();
        }));
    return chain.filter(exchange);
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 20;
  }

  private Mono<Void> attemptRefresh(Authentication authentication, String rawToken,
      ServerWebExchange exchange) {
    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      return Mono.empty();
    }
    Jwt jwt = jwtAuth.getToken();
    String tenantId = resolveTenant(exchange, jwt);
    return tokenRefreshService.refreshIfNecessary(tenantId, jwt, rawToken)
        .flatMap(refreshed -> {
          GatewaySecurityProperties.TokenRefresh cfg = properties.getTokenRefresh();
          String headerValue = cfg.isIncludeBearerPrefix() ? "Bearer " + refreshed : refreshed;
          exchange.getResponse().getHeaders().set(cfg.getResponseHeader(), headerValue);
          return Mono.empty();
        });
  }

  private String resolveBearer(HttpHeaders headers) {
    String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
    if (!StringUtils.hasText(auth)) {
      return null;
    }
    if (auth.toLowerCase().startsWith("bearer ")) {
      return auth.substring(7).trim();
    }
    return auth.trim();
  }

  private String resolveTenant(ServerWebExchange exchange, Jwt jwt) {
    String tenant = exchange.getAttribute(GatewayRequestAttributes.TENANT_ID);
    if (!StringUtils.hasText(tenant)) {
      tenant = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
    }
    if (!StringUtils.hasText(tenant) && jwt != null) {
      for (String claim : TENANT_CLAIMS) {
        String candidate = jwt.getClaimAsString(claim);
        if (StringUtils.hasText(candidate)) {
          tenant = candidate;
          break;
        }
      }
    }
    return trimToNull(tenant);
  }

  private static String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
