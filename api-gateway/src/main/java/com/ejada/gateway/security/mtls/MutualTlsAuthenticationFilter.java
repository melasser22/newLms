package com.ejada.gateway.security.mtls;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.GatewaySecurityProperties;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.gateway.security.GatewaySecurityMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Enforces mutual TLS for partner APIs by validating client certificates against tenant mappings.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class MutualTlsAuthenticationFilter implements WebFilter, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(MutualTlsAuthenticationFilter.class);

  private final GatewaySecurityProperties properties;
  private final PartnerCertificateService certificateService;
  private final GatewaySecurityMetrics metrics;
  private final ObjectMapper objectMapper;
  private final AntPathMatcher matcher = new AntPathMatcher();

  public MutualTlsAuthenticationFilter(GatewaySecurityProperties properties,
      PartnerCertificateService certificateService,
      GatewaySecurityMetrics metrics,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper) {
    this.properties = Objects.requireNonNull(properties, "properties");
    this.certificateService = Objects.requireNonNull(certificateService, "certificateService");
    this.metrics = Objects.requireNonNull(metrics, "metrics");
    ObjectMapper mapper = primaryObjectMapper != null ? primaryObjectMapper.getIfAvailable() : null;
    if (mapper == null && fallbackObjectMapper != null) {
      mapper = fallbackObjectMapper.getIfAvailable();
    }
    this.objectMapper = mapper != null ? mapper : new ObjectMapper().findAndRegisterModules();
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    GatewaySecurityProperties.MutualTls cfg = properties.getMutualTls();
    if (!cfg.isEnabled() || !requiresProtection(exchange, cfg)) {
      return chain.filter(exchange);
    }
    String tenantId = resolveTenant(exchange, cfg);
    if (!StringUtils.hasText(tenantId)) {
      metrics.incrementBlocked("mtls_tenant", null);
      return reject(exchange, HttpStatus.UNAUTHORIZED, "ERR_MTLS_TENANT", "Tenant context required for mTLS validation");
    }
    Certificate[] peerCertificates;
    try {
      peerCertificates = exchange.getRequest().getSslInfo() != null
          ? exchange.getRequest().getSslInfo().getPeerCertificates()
          : null;
    } catch (Exception ex) {
      LOGGER.warn("Failed to obtain peer certificates for tenant {}", tenantId, ex);
      metrics.incrementBlocked("mtls_certificate", tenantId);
      return reject(exchange, HttpStatus.FORBIDDEN, "ERR_MTLS_REQUIRED", "Valid client certificate required");
    }
    if (peerCertificates == null || peerCertificates.length == 0 || !(peerCertificates[0] instanceof X509Certificate certificate)) {
      metrics.incrementBlocked("mtls_certificate", tenantId);
      return reject(exchange, HttpStatus.FORBIDDEN, "ERR_MTLS_REQUIRED", "Valid client certificate required");
    }
    return certificateService.isCertificateTrusted(tenantId, (X509Certificate) peerCertificates[0])
        .flatMap(trusted -> {
          if (!trusted) {
            metrics.incrementBlocked("mtls_certificate", tenantId);
            return reject(exchange, HttpStatus.FORBIDDEN, "ERR_MTLS_DENIED", "Client certificate not trusted");
          }
          return chain.filter(exchange);
        });
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 2;
  }

  private boolean requiresProtection(ServerWebExchange exchange, GatewaySecurityProperties.MutualTls cfg) {
    String path = exchange.getRequest().getURI().getPath();
    for (String pattern : cfg.getPartnerRoutePatterns()) {
      if (StringUtils.hasText(pattern) && matcher.match(pattern, path)) {
        return true;
      }
    }
    return false;
  }

  private String resolveTenant(ServerWebExchange exchange, GatewaySecurityProperties.MutualTls cfg) {
    String tenant = exchange.getAttribute(GatewayRequestAttributes.TENANT_ID);
    if (!StringUtils.hasText(tenant)) {
      tenant = exchange.getRequest().getHeaders().getFirst(cfg.getTenantHeaderName());
    }
    if (!StringUtils.hasText(tenant)) {
      tenant = exchange.getRequest().getHeaders().getFirst(HeaderNames.X_TENANT_ID);
    }
    return trimToNull(tenant);
  }

  private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String code, String message) {
    var response = exchange.getResponse();
    if (response.isCommitted()) {
      return Mono.error(new BadCredentialsException(message));
    }
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

  private static String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
