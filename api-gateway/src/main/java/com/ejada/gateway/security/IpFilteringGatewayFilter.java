package com.ejada.gateway.security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.GatewaySecurityProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Enforces tenant specific IP whitelist/blacklist policies stored in Redis.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class IpFilteringGatewayFilter implements GlobalFilter, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(IpFilteringGatewayFilter.class);

  private final ReactiveStringRedisTemplate redisTemplate;
  private final GatewaySecurityProperties properties;
  private final GatewaySecurityMetrics metrics;
  private final ObjectMapper objectMapper;

  public IpFilteringGatewayFilter(
      ReactiveStringRedisTemplate redisTemplate,
      GatewaySecurityProperties properties,
      GatewaySecurityMetrics metrics,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
    this.properties = Objects.requireNonNull(properties, "properties");
    this.metrics = Objects.requireNonNull(metrics, "metrics");
    ObjectMapper mapper = (primaryObjectMapper != null) ? primaryObjectMapper.getIfAvailable() : null;
    if (mapper == null) {
      mapper = (fallbackObjectMapper != null) ? fallbackObjectMapper.getIfAvailable() : null;
    }
    this.objectMapper = Objects.requireNonNull(mapper, "objectMapper");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!properties.getIpFiltering().isEnabled()) {
      return chain.filter(exchange);
    }

    String tenantId = resolveTenant(exchange);
    if (!StringUtils.hasText(tenantId)) {
      return chain.filter(exchange);
    }

    String clientIp = resolveClientIp(exchange);
    if (!StringUtils.hasText(clientIp)) {
      return chain.filter(exchange);
    }

    GatewaySecurityProperties.IpFiltering cfg = properties.getIpFiltering();
    String blacklistKey = cfg.blacklistKey(tenantId);
    String whitelistKey = cfg.whitelistKey(tenantId);

    return redisTemplate.opsForSet().isMember(blacklistKey, clientIp)
        .defaultIfEmpty(false)
        .flatMap(isBlacklisted -> {
          if (isBlacklisted) {
            LOGGER.debug("Blocking request from {} for tenant {} due to blacklist", clientIp, tenantId);
            metrics.incrementBlocked("ip_blacklist", tenantId);
            return reject(exchange, "ERR_IP_BLOCKED", "IP address blocked", HttpStatus.FORBIDDEN);
          }
          return redisTemplate.opsForSet().members(whitelistKey)
              .collectList()
              .flatMap(members -> applyWhitelist(members, clientIp, tenantId, exchange, chain));
        });
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 3;
  }

  private Mono<Void> applyWhitelist(List<String> members, String clientIp, String tenantId,
      ServerWebExchange exchange, GatewayFilterChain chain) {
    if (members == null || members.isEmpty()) {
      return chain.filter(exchange);
    }
    Set<String> whitelist = members.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .collect(Collectors.toUnmodifiableSet());
    if (whitelist.contains(clientIp)) {
      return chain.filter(exchange);
    }
    LOGGER.debug("Blocking request from {} for tenant {} due to whitelist", clientIp, tenantId);
    metrics.incrementBlocked("ip_whitelist", tenantId);
    return reject(exchange, "ERR_IP_BLOCKED", "IP address blocked", HttpStatus.FORBIDDEN);
  }

  private Mono<Void> reject(ServerWebExchange exchange, String code, String message, HttpStatus status) {
    var response = exchange.getResponse();
    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    BaseResponse<Void> body = BaseResponse.error(code, message);
    byte[] payload;
    try {
      payload = objectMapper.writeValueAsBytes(body);
    } catch (Exception ex) {
      payload = body.toString().getBytes(StandardCharsets.UTF_8);
    }
    return response.writeWith(Mono.just(response.bufferFactory().wrap(payload)));
  }

  private String resolveTenant(ServerWebExchange exchange) {
    String tenant = exchange.getAttribute(GatewayRequestAttributes.TENANT_ID);
    if (!StringUtils.hasText(tenant)) {
      tenant = exchange.getRequest().getHeaders().getFirst(HeaderNames.X_TENANT_ID);
    }
    return trimToNull(tenant);
  }

  private String resolveClientIp(ServerWebExchange exchange) {
    String forwarded = exchange.getRequest().getHeaders().getFirst(HeaderNames.CLIENT_IP);
    if (StringUtils.hasText(forwarded)) {
      String candidate = forwarded.split(",")[0].trim();
      if (StringUtils.hasText(candidate)) {
        return candidate;
      }
    }
    InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
    if (remoteAddress == null) {
      return null;
    }
    if (remoteAddress.getAddress() != null) {
      return remoteAddress.getAddress().getHostAddress();
    }
    return remoteAddress.getHostString();
  }

  private static String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
