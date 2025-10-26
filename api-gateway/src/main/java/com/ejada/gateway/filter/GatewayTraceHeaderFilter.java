package com.ejada.gateway.filter;

import java.util.UUID;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Adds an {@code X-Gateway-Trace} header with a random UUID value to every request
 * that flows through the gateway. This replaces the previous configuration based
 * solution that relied on SpEL type lookups, which are now restricted by default in
 * Spring Boot 3.4.
 */
@Component
public class GatewayTraceHeaderFilter implements GlobalFilter, Ordered {

    static final String TRACE_HEADER = "X-Gateway-Trace";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        ServerHttpRequest mutatedRequest = exchange.getRequest()
            .mutate()
            .headers(httpHeaders -> httpHeaders.add(TRACE_HEADER, UUID.randomUUID().toString()))
            .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
