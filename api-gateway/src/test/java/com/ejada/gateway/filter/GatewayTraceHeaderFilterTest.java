package com.ejada.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

class GatewayTraceHeaderFilterTest {

    private GatewayTraceHeaderFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GatewayTraceHeaderFilter();
    }

    @Test
    void shouldAddRandomTraceHeaderWhenMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());
        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();

        GatewayFilterChain chain = exchangeArg -> {
            capturedExchange.set(exchangeArg);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        ServerWebExchange mutatedExchange = capturedExchange.get();
        assertThat(mutatedExchange).isNotNull();

        String traceHeader = mutatedExchange.getRequest().getHeaders().getFirst(GatewayTraceHeaderFilter.TRACE_HEADER);
        assertThat(traceHeader).isNotBlank();
        assertThat(UUID.fromString(traceHeader)).isInstanceOf(UUID.class);
    }

    @Test
    void shouldAppendTraceHeaderWhenAlreadyPresent() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
            .header(GatewayTraceHeaderFilter.TRACE_HEADER, "existing-value")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();

        GatewayFilterChain chain = exchangeArg -> {
            capturedExchange.set(exchangeArg);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        ServerWebExchange mutatedExchange = capturedExchange.get();
        assertThat(mutatedExchange).isNotNull();

        assertThat(mutatedExchange.getRequest().getHeaders().get(GatewayTraceHeaderFilter.TRACE_HEADER))
            .hasSize(2)
            .anyMatch("existing-value"::equals)
            .anySatisfy(value -> assertThat(UUID.fromString(value)).isInstanceOf(UUID.class));
    }

}
