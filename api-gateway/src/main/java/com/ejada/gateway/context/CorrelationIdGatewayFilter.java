package com.ejada.gateway.context;

import com.ejada.common.constants.HeaderNames;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import com.ejada.starter_core.web.FilterSkipUtils;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Ensures every request flowing through the gateway has a correlation id.
 *
 * <p>The filter simply mirrors the servlet starter's behaviour: if the client
 * provides a correlation id header it is reused, otherwise a new identifier is
 * generated (when configured). The value is exposed via the exchange
 * attributes so downstream components (rate-limiters, WebClient) can reuse it
 * without re-parsing headers.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdGatewayFilter implements WebFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdGatewayFilter.class);

  private final CoreAutoConfiguration.CoreProps props;

  public CorrelationIdGatewayFilter(CoreAutoConfiguration.CoreProps props) {
    this.props = Objects.requireNonNull(props, "props");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    var correlationProps = props.getCorrelation();
    if (!correlationProps.isEnabled()) {
      return chain.filter(exchange);
    }

    String path = exchange.getRequest().getPath().pathWithinApplication().value();
    if (FilterSkipUtils.shouldSkip(path, correlationProps.getSkipPatterns())) {
      return chain.filter(exchange);
    }

    final String headerName = correlationProps.getHeaderName();
    String correlationId = trimToNull(exchange.getRequest().getHeaders().getFirst(headerName));
    if (!StringUtils.hasText(correlationId) && correlationProps.isGenerateIfMissing()) {
      correlationId = UUID.randomUUID().toString();
      LOGGER.trace("Generated correlation id {} for request {}", correlationId, path);
    }

    if (!StringUtils.hasText(correlationId)) {
      return chain.filter(exchange);
    }

    ServerWebExchange mutatedExchange = exchange;
    if (!StringUtils.hasText(exchange.getRequest().getHeaders().getFirst(headerName))) {
      ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
          .header(headerName, correlationId)
          .build();
      mutatedExchange = exchange.mutate().request(mutatedRequest).build();
    }

    mutatedExchange.getAttributes().put(GatewayRequestAttributes.CORRELATION_ID, correlationId);
    mutatedExchange.getAttributes().putIfAbsent(HeaderNames.CORRELATION_ID, correlationId);

    if (correlationProps.isEchoResponseHeader() && StringUtils.hasText(correlationId)) {
      final String resolvedCorrelationId = correlationId;
      final ServerWebExchange exchangeToFilter = mutatedExchange;
      exchangeToFilter.getResponse().beforeCommit(() -> {
        exchangeToFilter.getResponse().getHeaders().set(headerName, resolvedCorrelationId);
        return Mono.empty();
      });
    }

    return chain.filter(mutatedExchange);
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}

