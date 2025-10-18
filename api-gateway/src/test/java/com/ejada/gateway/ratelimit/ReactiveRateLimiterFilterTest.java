package com.ejada.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.gateway.config.GatewayRateLimitProperties;
import com.ejada.gateway.config.GatewayTracingProperties;
import com.ejada.gateway.observability.GatewayTracingHelper;
import com.ejada.shared_starter_ratelimit.RateLimitProps;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ReactiveRateLimiterFilterTest {

    private ReactiveRateLimiterFilter filter;
    private ReactiveStringRedisTemplate redisTemplate;
    private GatewayRateLimitProperties gatewayRateLimitProperties;
    private GatewayTracingHelper tracingHelper;
    private Map<String, WindowCounter> counters;

    @BeforeEach
    void setUp() {
        this.redisTemplate = org.mockito.Mockito.mock(ReactiveStringRedisTemplate.class);
        this.counters = new ConcurrentHashMap<>();
        stubRateLimitScript();
        RateLimitProps props = createProps(2, 2);
        this.gatewayRateLimitProperties = new GatewayRateLimitProperties();
        gatewayRateLimitProperties.setBurstMultiplier(1.0d);
        KeyResolver keyResolver = exchange -> Mono.just("tenant-a");
        this.tracingHelper = new GatewayTracingHelper(null, new GatewayTracingProperties());
        this.filter = new ReactiveRateLimiterFilter(redisTemplate, props, keyResolver, new ObjectMapper().findAndRegisterModules(),
                gatewayRateLimitProperties, new SimpleMeterRegistry(), tracingHelper);
    }

    @Test
    void allowsRequestsWithinCapacity() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/items").build());
        WebFilterChain chain = serverWebExchange -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("2");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("1");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Policy")).isEqualTo("default");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Reset")).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Window")).isEqualTo("60");
    }

    @Test
    void rejectsWhenCapacityExceeded() {
        MockServerWebExchange exchange1 = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/data").build());
        MockServerWebExchange exchange2 = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/data").build());
        MockServerWebExchange exchange3 = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/data").build());
        WebFilterChain chain = serverWebExchange -> Mono.empty();

        StepVerifier.create(filter.filter(exchange1, chain)).verifyComplete();
        StepVerifier.create(filter.filter(exchange2, chain)).verifyComplete();

        StepVerifier.create(filter.filter(exchange3, chain)).verifyComplete();

        assertThat(exchange3.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        String body = exchange3.getResponse().getBodyAsString().block();
        assertThat(body).contains("ERR_RATE_LIMIT");
    }

    @Test
    void slidingWindowEnforcesLimitsUnderConcurrency() {
        RateLimitProps props = createProps(5, 5);
        GatewayRateLimitProperties gatewayProps = new GatewayRateLimitProperties();
        gatewayProps.setBurstMultiplier(1.0d);
        KeyResolver keyResolver = exchange -> Mono.just("tenant-b");
        ReactiveRateLimiterFilter concurrentFilter = new ReactiveRateLimiterFilter(redisTemplate, props, keyResolver,
                new ObjectMapper().findAndRegisterModules(), gatewayProps, new SimpleMeterRegistry(), tracingHelper);

        WebFilterChain chain = serverWebExchange -> Mono.empty();

        var exchanges = IntStream.range(0, 6)
                .mapToObj(i -> MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/data" + i).build()))
                .toList();

        StepVerifier.create(Flux.fromIterable(exchanges)
                        .flatMap(exchange -> concurrentFilter.filter(exchange, chain).thenReturn(exchange)))
                .expectNextCount(6)
                .verifyComplete();

        long rejected = exchanges.stream()
                .filter(exchange -> HttpStatus.TOO_MANY_REQUESTS.equals(exchange.getResponse().getStatusCode()))
                .count();
        long allowed = exchanges.stream()
                .filter(exchange -> exchange.getResponse().getStatusCode() == null)
                .count();

        assertThat(allowed).isEqualTo(5);
        assertThat(rejected).isEqualTo(1);
    }

    private RateLimitProps createProps(int requestsPerMinute, int burstCapacity) {
        RateLimitProps props = new RateLimitProps();
        RateLimitProps.TierProperties tier = new RateLimitProps.TierProperties();
        tier.setRequestsPerMinute(requestsPerMinute);
        tier.setBurstCapacity(burstCapacity);
        props.setDefaultTier("custom");
        props.setTiers(Map.of("custom", tier));
        RateLimitProps.StrategyProperties strategy = new RateLimitProps.StrategyProperties();
        strategy.setName("tenant");
        strategy.setEnabled(true);
        strategy.setDimensions(List.of(RateLimitProps.Dimension.TENANT));
        RateLimitProps.MultiDimensionalProperties multidimensional = new RateLimitProps.MultiDimensionalProperties();
        multidimensional.setStrategies(List.of(strategy));
        props.setMultidimensional(multidimensional);
        props.applyDefaults();
        return props;
    }

    private void stubRateLimitScript() {
        org.mockito.Mockito.lenient()
                .when(redisTemplate.execute(org.mockito.ArgumentMatchers.<RedisScript<List<?>>>any(),
                        org.mockito.ArgumentMatchers.<List<String>>any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenAnswer(rateLimitAnswer());
    }

    private Answer<Flux<List<?>>> rateLimitAnswer() {
        return invocation -> {
            @SuppressWarnings("unchecked")
            List<String> keys = invocation.getArgument(1);
            Object[] args = Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length);
            Object[] scriptArgs = (args.length == 1 && args[0] instanceof Object[])
                    ? (Object[]) args[0]
                    : args;
            String rateKey = keys.get(0);
            long nowMillis = Long.parseLong(scriptArgs[1].toString());
            long windowMillis = Long.parseLong(scriptArgs[2].toString());
            long capacity = Long.parseLong(scriptArgs[3].toString());
            long burstCapacity = Long.parseLong(scriptArgs[4].toString());
            WindowCounter counter = counters.computeIfAbsent(rateKey, key -> new WindowCounter(nowMillis, windowMillis));
            counter.resetIfExpired(nowMillis, windowMillis);
            long totalAllowed = Math.max(capacity, burstCapacity);
            boolean allowed = counter.increment(totalAllowed);
            long remaining = Math.max(0, totalAllowed - counter.requests.get());
            long reset = counter.windowStart + windowMillis;
            boolean burstUsed = counter.requests.get() > capacity;
            long baseRemaining = Math.max(0, capacity - counter.requests.get());
            long burstRemaining = Math.max(0, totalAllowed - capacity - Math.max(0, counter.requests.get() - capacity));
            List<Object> result = new ArrayList<>();
            result.add(allowed);
            result.add(remaining);
            result.add(reset);
            result.add("event");
            result.add(burstUsed);
            result.add(baseRemaining);
            result.add(burstRemaining);
            return Flux.just(result);
        };
    }

    private static final class WindowCounter {
        private long windowStart;
        private final AtomicInteger requests = new AtomicInteger();

        private WindowCounter(long windowStart, long windowMillis) {
            this.windowStart = windowStart;
        }

        private void resetIfExpired(long nowMillis, long windowMillis) {
            if (nowMillis - windowStart >= windowMillis) {
                windowStart = nowMillis;
                requests.set(0);
            }
        }

        private boolean increment(long capacity) {
            int current = requests.incrementAndGet();
            return current <= capacity;
        }
    }
}
