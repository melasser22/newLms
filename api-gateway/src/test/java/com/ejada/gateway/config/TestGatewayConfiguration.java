package com.ejada.gateway.config;

import com.ejada.gateway.config.TestGatewayConfiguration.NoOpReactiveCircuitBreakerFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import reactor.core.publisher.Mono;

/**
 * Shared beans used across gateway test slices to provide deterministic
 * authentication and circuit breaker behaviour. The gateway relies on
 * JWT authentication and Resilience4J at runtime; these test doubles keep
 * the application context lightweight while still exercising the majority
 * of the request pipeline.
 */
@TestConfiguration
public class TestGatewayConfiguration {

  @Bean
  @Primary
  JwtDecoder jwtDecoder() {
    return token -> buildJwt(token);
  }

  @Bean
  @Primary
  ReactiveJwtDecoder reactiveJwtDecoder() {
    return token -> Mono.just(buildJwt(token));
  }

  @Bean
  @Primary
  ReactiveJwtAuthenticationConverterAdapter reactiveJwtAuthenticationConverterAdapter() {
    JwtAuthenticationConverter mutableConverter = new JwtAuthenticationConverter();
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthorityPrefix("ROLE_");
    mutableConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return new ReactiveJwtAuthenticationConverterAdapter(mutableConverter);
  }

  @Bean
  @Primary
  NoOpReactiveCircuitBreakerFactory noopReactiveCircuitBreakerFactory() {
    return new NoOpReactiveCircuitBreakerFactory();
  }

  private Jwt buildJwt(String token) {
    return new Jwt(token, Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS),
        Map.of("alg", "none"), Map.of(
            "sub", "integration-user",
            "tenant_id", "integration-tenant",
            "scope", "gateway.read"
        ));
  }

  /**
   * Simplified circuit breaker factory used by integration tests.
   */
  static class NoOpReactiveCircuitBreakerFactory extends org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory<Object, Object> {

    @Override
    protected org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker create(String id, Object config) {
      return reactiveCircuitBreaker(id);
    }

    @Override
    public org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker create(String id) {
      return reactiveCircuitBreaker(id);
    }

    private org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker reactiveCircuitBreaker(String id) {
      return (toRun, fallback) -> toRun;
    }
  }
}
