package com.ejada.starter_core.logging;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.CorrelationContextUtil;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

public class CorrelationIdFilter implements WebFilter, Ordered {

    private final String headerName;
    private final String mdcKey;
    private final boolean generateIfMissing;
    private final boolean echoResponseHeader;
    private final String[] skipPatterns;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public CorrelationIdFilter(
            String headerName,
            String mdcKey,
            boolean generateIfMissing,
            boolean echoResponseHeader,
            String[] skipPatterns
    ) {
        this.headerName = Objects.requireNonNullElse(headerName, HeaderNames.CORRELATION_ID);
        this.mdcKey = Objects.requireNonNullElse(mdcKey, HeaderNames.CORRELATION_ID);
        this.generateIfMissing = generateIfMissing;
        this.echoResponseHeader = echoResponseHeader;
        this.skipPatterns = (skipPatterns != null && skipPatterns.length > 0)
                ? skipPatterns
                : new String[]{"/actuator/**", "/swagger-ui/**", "/v3/api-docs/**", "/static/**", "/webjars/**", "/error", "/favicon.ico"};
    }

    @Override
    public int getOrder() {
        return order;
    }

    private int order = Ordered.HIGHEST_PRECEDENCE;

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String uri = exchange.getRequest().getURI().getPath();
        for (String p : skipPatterns) {
            if (matcher.match(p, uri)) {
                return chain.filter(exchange);
            }
        }

        String correlationId = exchange.getRequest().getHeaders().getFirst(headerName);
        if (isBlank(correlationId) && generateIfMissing) {
            correlationId = UUID.randomUUID().toString();
        }

        if (!isBlank(correlationId)) {
            MDC.put(mdcKey, correlationId);
            CorrelationContextUtil.put(CorrelationContextUtil.CORRELATION_ID, correlationId);
            if (echoResponseHeader) {
                exchange.getResponse().getHeaders().set(headerName, correlationId);
            }
        }

        return chain.filter(exchange).doFinally(s -> {
            MDC.remove(mdcKey);
            CorrelationContextUtil.clear();
        });
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
