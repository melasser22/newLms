package com.ejada.gateway.context;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.common.context.TenantMdcUtil;
import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.context.GatewayRequestAttributes;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import com.ejada.starter_core.tenant.TenantResolution;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Reactive equivalent of {@code ContextFilter}. It reuses the shared
 * configuration properties to populate correlation and tenant details in MDC
 * and {@link ContextManager}.
 */
public class ReactiveRequestContextFilter implements WebFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveRequestContextFilter.class);

  private final CoreAutoConfiguration.CoreProps props;
  private final ObjectMapper objectMapper;
  private final String[] skipPatterns;

  public ReactiveRequestContextFilter(CoreAutoConfiguration.CoreProps props, ObjectMapper objectMapper) {
    this.props = Objects.requireNonNull(props, "props");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    Set<String> merged = new LinkedHashSet<>();
    if (props.getCorrelation().getSkipPatterns() != null) {
      Stream.of(props.getCorrelation().getSkipPatterns()).filter(StringUtils::hasText).forEach(merged::add);
    }
    if (props.getTenant().getSkipPatterns() != null) {
      Stream.of(props.getTenant().getSkipPatterns()).filter(StringUtils::hasText).forEach(merged::add);
    }
    this.skipPatterns = merged.toArray(String[]::new);
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getPath().pathWithinApplication().value();
    if (shouldSkip(path)) {
      return chain.filter(exchange);
    }

    Mono<Authentication> authentication = exchange.getPrincipal()
        .filter(Authentication.class::isInstance)
        .cast(Authentication.class)
        .cache(Duration.ofSeconds(1));

    RequestContextState state = new RequestContextState(props);
    state.setTraceHeaders(extractTraceHeaders(exchange));
    String correlationId = exchange.getAttribute(GatewayRequestAttributes.CORRELATION_ID);
    if (!StringUtils.hasText(correlationId)) {
      correlationId = resolveCorrelationId(exchange);
      if (StringUtils.hasText(correlationId)) {
        exchange.getAttributes().put(GatewayRequestAttributes.CORRELATION_ID, correlationId);
      }
    }
    state.setCorrelationId(correlationId);

    Mono<TenantResolution> tenantResolutionMono;
    String preResolvedTenant = exchange.getAttribute(GatewayRequestAttributes.TENANT_ID);
    if (StringUtils.hasText(preResolvedTenant)) {
      tenantResolutionMono = Mono.just(validateTenant(preResolvedTenant));
    } else {
      tenantResolutionMono = resolveTenant(exchange, authentication);
    }

    return tenantResolutionMono
        .flatMap(resolution -> {
          if (resolution.isInvalid()) {
            return rejectTenant(exchange, resolution.rawValue());
          }
          state.setTenant(resolution.hasTenant() ? resolution.tenantId() : null);
          return authentication
              .map(Optional::of)
              .defaultIfEmpty(Optional.empty())
              .flatMap(optionalAuth -> {
                state.setAuthentication(optionalAuth.orElse(null));
                state.apply(exchange);
                return chain.filter(exchange)
                    .contextWrite(ctx -> enrichContext(ctx, state))
                    .doFinally(signal -> state.cleanup());
              });
        });
  }

  private Context enrichContext(Context context, RequestContextState state) {
    Context updated = context;
    if (state.getCorrelationId() != null) {
      updated = updated.put(HeaderNames.CORRELATION_ID, state.getCorrelationId());
    }
    if (state.getTenant() != null) {
      updated = updated.put(HeaderNames.X_TENANT_ID, state.getTenant());
    }
    if (state.getUserId() != null) {
      updated = updated.put(HeaderNames.USER_ID, state.getUserId());
    }
    if (!state.getTraceHeaders().isEmpty()) {
      updated = updated.put(GatewayRequestAttributes.TRACE_HEADERS, state.getTraceHeaders());
    }
    return updated;
  }

  private java.util.Map<String, String> extractTraceHeaders(ServerWebExchange exchange) {
    HttpHeaders headers = exchange.getRequest().getHeaders();
    java.util.Map<String, String> extracted = new LinkedHashMap<>();
    copyTraceHeader(headers, "X-B3-TraceId", extracted);
    copyTraceHeader(headers, "X-B3-SpanId", extracted);
    copyTraceHeader(headers, "X-B3-ParentSpanId", extracted);
    copyTraceHeader(headers, "X-B3-Sampled", extracted);
    copyTraceHeader(headers, "X-B3-Flags", extracted);
    copyTraceHeader(headers, "traceparent", extracted);
    copyTraceHeader(headers, "tracestate", extracted);
    if (extracted.isEmpty()) {
      return Collections.emptyMap();
    }
    return extracted;
  }

  private void copyTraceHeader(HttpHeaders headers, String name, java.util.Map<String, String> target) {
    String value = trimToNull(headers.getFirst(name));
    if (value != null) {
      target.put(name, value);
    }
  }

  private boolean shouldSkip(String path) {
    if (!StringUtils.hasText(path)) {
      return false;
    }
    return com.ejada.starter_core.web.FilterSkipUtils.shouldSkip(path, skipPatterns);
  }

  private String resolveCorrelationId(ServerWebExchange exchange) {
    CoreAutoConfiguration.CoreProps.Correlation cfg = props.getCorrelation();
    if (!cfg.isEnabled()) {
      return null;
    }
    String headerName = cfg.getHeaderName();
    String correlationId = trimToNull(exchange.getRequest().getHeaders().getFirst(headerName));
    if (correlationId == null && cfg.isGenerateIfMissing()) {
      correlationId = UUID.randomUUID().toString();
    }
    if (correlationId != null && cfg.isEchoResponseHeader()) {
      exchange.getResponse().getHeaders().set(headerName, correlationId);
    }
    return correlationId;
  }

  private Mono<TenantResolution> resolveTenant(ServerWebExchange exchange, Mono<Authentication> authentication) {
    CoreAutoConfiguration.CoreProps.Tenant cfg = props.getTenant();
    if (!cfg.isEnabled()) {
      return Mono.just(TenantResolution.absent());
    }
    String fromHeader = trimToNull(exchange.getRequest().getHeaders().getFirst(cfg.getHeaderName()));
    String fromQuery = trimToNull(exchange.getRequest().getQueryParams().getFirst(cfg.getQueryParam()));

    Mono<Optional<String>> fromJwt = Mono.just(Optional.<String>empty());
    if (cfg.isResolveFromJwt()) {
      fromJwt = authentication
          .flatMap(auth -> Mono.justOrEmpty(extractTenantFromJwt(auth, cfg.getJwtClaimNames())))
          .map(Optional::of)
          .switchIfEmpty(Mono.just(Optional.empty()));
    }

    boolean preferHeader = cfg.isPreferHeaderOverJwt();

    return fromJwt.map(optionalJwt -> {
      String jwt = optionalJwt.orElse(null);
      String candidate = preferHeader
          ? selectTenant(fromHeader, fromQuery, jwt)
          : selectTenant(jwt, fromHeader, fromQuery);
      if (candidate == null) {
        return TenantResolution.absent();
      }
      return validateTenant(candidate);
    });
  }

  private String selectTenant(String first, String second, String third) {
    if (StringUtils.hasText(first)) {
      return first.trim();
    }
    if (StringUtils.hasText(second)) {
      return second.trim();
    }
    if (StringUtils.hasText(third)) {
      return third.trim();
    }
    return null;
  }

  private String extractTenantFromJwt(Authentication auth, String[] claimNames) {
    if (auth instanceof JwtAuthenticationToken jwtAuthenticationToken) {
      for (String claim : claimNames) {
        Object value = jwtAuthenticationToken.getToken().getClaims().get(claim);
        if (value != null) {
          String tenant = Objects.toString(value, null);
          if (StringUtils.hasText(tenant)) {
            return tenant.trim();
          }
        }
      }
    }
    return null;
  }

  private TenantResolution validateTenant(String candidate) {
    if (!StringUtils.hasText(candidate)) {
      return TenantResolution.absent();
    }
    String trimmed = candidate.trim();
    if (!trimmed.matches("[A-Za-z0-9_-]{1,36}")) {
      return TenantResolution.invalid(trimmed);
    }
    return TenantResolution.present(trimmed);
  }

  private Mono<Void> rejectTenant(ServerWebExchange exchange, String rawValue) {
    LOGGER.warn("Invalid tenant identifier: {}", rawValue);
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.BAD_REQUEST);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    BaseResponse<Void> body = BaseResponse.error("ERR_INVALID_TENANT", "Invalid " + HeaderNames.X_TENANT_ID);
    try {
      byte[] bytes = objectMapper.writeValueAsBytes(body);
      return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    } catch (JsonProcessingException ex) {
      byte[] fallback = ("{\"status\":\"ERROR\",\"code\":\"ERR_INVALID_TENANT\",\"message\":\"Invalid "
          + HeaderNames.X_TENANT_ID + "\"}").getBytes(StandardCharsets.UTF_8);
      return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
    }
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /**
   * Holds the request-scoped context and is responsible for applying and
   * cleaning up MDC/thread-local state.
   */
  static final class RequestContextState {

    private final CoreAutoConfiguration.CoreProps props;
    private String correlationId;
    private String tenant;
    private Authentication authentication;
    private ContextManager.Tenant.Scope tenantScope;
    private java.util.Map<String, String> traceHeaders = java.util.Collections.emptyMap();

    RequestContextState(CoreAutoConfiguration.CoreProps props) {
      this.props = props;
    }

    void setCorrelationId(String correlationId) {
      this.correlationId = correlationId;
    }

    void setTenant(String tenant) {
      this.tenant = tenant;
    }

    void setAuthentication(Authentication authentication) {
      this.authentication = authentication;
    }

    void setTraceHeaders(java.util.Map<String, String> traceHeaders) {
      if (traceHeaders == null || traceHeaders.isEmpty()) {
        this.traceHeaders = java.util.Collections.emptyMap();
        return;
      }
      this.traceHeaders = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(traceHeaders));
    }

    String getCorrelationId() {
      return correlationId;
    }

    String getTenant() {
      return tenant;
    }

    String getUserId() {
      return authentication != null ? trimToNull(authentication.getName()) : null;
    }

    java.util.Map<String, String> getTraceHeaders() {
      return traceHeaders;
    }

    void apply(ServerWebExchange exchange) {
      if (correlationId != null && props.getCorrelation().isEnabled()) {
        MDC.put(props.getCorrelation().getMdcKey(), correlationId);
        if (!HeaderNames.CORRELATION_ID.equals(props.getCorrelation().getMdcKey())) {
          MDC.put(HeaderNames.CORRELATION_ID, correlationId);
        }
        ContextManager.setCorrelationId(correlationId);
        exchange.getAttributes().put(GatewayRequestAttributes.CORRELATION_ID, correlationId);
      }

      if (tenant != null && props.getTenant().isEnabled()) {
        MDC.put(props.getTenant().getMdcKey(), tenant);
        TenantMdcUtil.setTenantId(tenant);
        tenantScope = ContextManager.Tenant.openScope(tenant);
        if (props.getTenant().isEchoResponseHeader()) {
          exchange.getResponse().getHeaders().set(props.getTenant().getHeaderName(), tenant);
        }
        exchange.getAttributes().put(GatewayRequestAttributes.TENANT_ID, tenant);
      }

      String userId = getUserId();
      if (StringUtils.hasText(userId)) {
        MDC.put(HeaderNames.USER_ID, userId);
        ContextManager.setUserId(userId);
      }

      if (!traceHeaders.isEmpty()) {
        exchange.getAttributes().put(GatewayRequestAttributes.TRACE_HEADERS, traceHeaders);
      }
    }

    void cleanup() {
      if (correlationId != null && props.getCorrelation().isEnabled()) {
        MDC.remove(props.getCorrelation().getMdcKey());
        if (!HeaderNames.CORRELATION_ID.equals(props.getCorrelation().getMdcKey())) {
          MDC.remove(HeaderNames.CORRELATION_ID);
        }
        ContextManager.clearCorrelationId();
      }
      if (tenantScope != null) {
        try {
          tenantScope.close();
        } catch (Exception ex) {
          LOGGER.warn("Failed to close tenant scope", ex);
        }
      }
      if (props.getTenant().isEnabled()) {
        MDC.remove(props.getTenant().getMdcKey());
        TenantMdcUtil.clear();
      }
      String userId = getUserId();
      if (StringUtils.hasText(userId)) {
        MDC.remove(HeaderNames.USER_ID);
        ContextManager.clearUserId();
      }
    }
  }
}
