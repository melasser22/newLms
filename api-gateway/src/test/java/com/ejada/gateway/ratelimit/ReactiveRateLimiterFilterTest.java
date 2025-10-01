package com.ejada.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.shared_starter_ratelimit.RateLimitProps;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ReactiveRateLimiterFilterTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7").withExposedPorts(6379);

    private ReactiveRateLimiterFilter filter;
    private ReactiveRedisConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        this.connectionFactory = factory;
        ReactiveStringRedisTemplate template = new ReactiveStringRedisTemplate(factory);
        RateLimitProps props = new RateLimitProps();
        props.setCapacity(2);
        props.setRefillPerMinute(2);
        props.setKeyStrategy("tenant");
        this.filter = new ReactiveRateLimiterFilter(template, props, new ObjectMapper());
        flushRedis();
    }

    @Test
    void allowsRequestsWithinCapacity() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/items").build());
        WebFilterChain chain = serverWebExchange -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("2");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("1");
    }

    @Test
    void rejectsWhenCapacityExceeded() {
        MockServerWebExchange exchange1 = MockServerWebExchange.from(MockServerHttpRequest.get("/api/data").build());
        MockServerWebExchange exchange2 = MockServerWebExchange.from(MockServerHttpRequest.get("/api/data").build());
        MockServerWebExchange exchange3 = MockServerWebExchange.from(MockServerHttpRequest.get("/api/data").build());
        WebFilterChain chain = serverWebExchange -> Mono.empty();

        StepVerifier.create(filter.filter(exchange1, chain)).verifyComplete();
        StepVerifier.create(filter.filter(exchange2, chain)).verifyComplete();

        StepVerifier.create(filter.filter(exchange3, chain)).verifyComplete();

        assertThat(exchange3.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        String body = exchange3.getResponse().getBodyAsString().block();
        assertThat(body).contains("ERR_RATE_LIMIT");
    }

    private void flushRedis() {
        try (var connection = connectionFactory.getReactiveConnection()) {
            connection.serverCommands().flushAll().block();
        }
    }
}
