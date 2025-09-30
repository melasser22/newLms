package com.ejada.gateway.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.ContextView;

class ReactiveRequestContextFilterTest {

    private ReactiveRequestContextFilter filter;

    @BeforeEach
    void setUp() {
        CoreAutoConfiguration.CoreProps props = new CoreAutoConfiguration.CoreProps();
        props.getCorrelation().setSkipPatterns(new String[0]);
        props.getTenant().setSkipPatterns(new String[0]);
        filter = new ReactiveRequestContextFilter(props, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        ContextManager.clearHeaders();
    }

    @Test
    void populatesContextAndReactorContext() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/data")
                .header(HeaderNames.CORRELATION_ID, "corr-123")
                .header(HeaderNames.X_TENANT_ID, "tenant-1")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ContextView> contextRef = new AtomicReference<>();

        WebFilterChain chain = serverExchange -> Mono.deferContextual(ctx -> {
            contextRef.set(ctx);
            assertThat(ContextManager.getCorrelationId()).isEqualTo("corr-123");
            assertThat(ContextManager.Tenant.get()).isEqualTo("tenant-1");
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst(HeaderNames.CORRELATION_ID)).isEqualTo("corr-123");
        assertThat(contextRef.get()).isNotNull();
        assertThat((String) contextRef.get().get(HeaderNames.CORRELATION_ID)).isEqualTo("corr-123");
        assertThat((String) contextRef.get().get(HeaderNames.X_TENANT_ID)).isEqualTo("tenant-1");
    }

    @Test
    void rejectsInvalidTenantIdentifier() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/data")
                .header(HeaderNames.CORRELATION_ID, "corr-789")
                .header(HeaderNames.X_TENANT_ID, "tenant@!invalid")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicBoolean invoked = new AtomicBoolean(false);

        WebFilterChain chain = serverExchange -> {
            invoked.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(invoked.get()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = exchange.getResponse().getBodyAsString().block(Duration.ofSeconds(3));
        assertThat(body).contains("ERR_INVALID_TENANT");
    }
}
