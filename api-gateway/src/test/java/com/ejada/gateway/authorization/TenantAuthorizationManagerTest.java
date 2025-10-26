package com.ejada.gateway.authorization;

import com.ejada.gateway.config.GatewayRateLimitProperties;
import com.ejada.gateway.subscription.SubscriptionCacheService;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import com.ejada.starter_security.Role;
import com.ejada.starter_security.SharedSecurityProps;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    AuthorizationContext context = new AuthorizationContext(exchange);

    Mono<AuthorizationDecision> decision = manager.check(Mono.empty(), context);

    StepVerifier.create(decision)
        .expectNextMatches(AuthorizationDecision::isGranted)
        .verifyComplete();
  }

  @Test
  void ejadaOfficerWithoutTenantInformationBypassesTenantChecks() {
    CoreAutoConfiguration.CoreProps coreProps = new CoreAutoConfiguration.CoreProps();
    coreProps.getTenant().setEnabled(true);

    SharedSecurityProps securityProps = new SharedSecurityProps();

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
        .post("/api/v1/superadmin/admins/first-login")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    AuthorizationContext context = new AuthorizationContext(exchange);

    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        "superadmin",
        "n/a",
        List.of(new SimpleGrantedAuthority(Role.EJADA_OFFICER.getAuthority())));

    Mono<AuthorizationDecision> decision = manager.check(Mono.just(authentication), context);

    StepVerifier.create(decision)
        .expectNextMatches(AuthorizationDecision::isGranted)
        .verifyComplete();

    assertEquals("super-admin", exchange.getResponse().getHeaders().getFirst("X-Tenant-Verified"));
    Mockito.verifyNoInteractions(subscriptionCacheService);
  }
}
