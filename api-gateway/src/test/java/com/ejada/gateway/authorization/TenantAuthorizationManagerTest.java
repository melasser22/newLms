package com.ejada.gateway.authorization;

import com.ejada.gateway.config.GatewayRateLimitProperties;
import com.ejada.gateway.subscription.SubscriptionCacheService;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import com.ejada.starter_security.SharedSecurityProps;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class TenantAuthorizationManagerTest {

  @Test
  void permitAllPathDoesNotRequireAuthentication() {
    CoreAutoConfiguration.CoreProps coreProps = new CoreAutoConfiguration.CoreProps();
    coreProps.getTenant().setEnabled(true);
    coreProps.getTenant().setSkipPatterns(new String[]{"/api/auth/**"});

    SharedSecurityProps securityProps = new SharedSecurityProps();
    securityProps.getResourceServer().setPermitAll(new String[]{"/api/auth/**"});

    ReactiveStringRedisTemplate redisTemplate = Mockito.mock(ReactiveStringRedisTemplate.class);
    SubscriptionCacheService subscriptionCacheService = Mockito.mock(SubscriptionCacheService.class);
    GatewayRateLimitProperties rateLimitProperties = new GatewayRateLimitProperties();

    TenantAuthorizationManager manager = new TenantAuthorizationManager(
        coreProps,
        securityProps,
        redisTemplate,
        subscriptionCacheService,
        rateLimitProperties,
        new ObjectMapper());

    MockServerHttpRequest request = MockServerHttpRequest
        .method(HttpMethod.POST, "/api/auth/admin/login")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    AuthorizationContext context = AuthorizationContext.from(exchange);

    Mono<AuthorizationDecision> decision = manager.check(Mono.empty(), context);

    StepVerifier.create(decision)
        .expectNextMatches(AuthorizationDecision::isGranted)
        .verifyComplete();
  }
}
