package com.ejada.gateway.config;

import com.ejada.starter_security.SharedSecurityProps;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import java.util.List;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Reactive security configuration that adapts the shared servlet-based starter
 * to Spring WebFlux.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfiguration {

  @Bean
  public ReactiveJwtDecoder reactiveJwtDecoder(JwtDecoder jwtDecoder) {
    return token -> Mono.fromCallable(() -> jwtDecoder.decode(token))
        .subscribeOn(Schedulers.boundedElastic());
  }

  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
      SharedSecurityProps props,
      CorsConfigurationSource corsConfigurationSource,
      ReactiveJwtDecoder reactiveJwtDecoder,
      JwtAuthenticationConverter jwtAuthenticationConverter) {
    var rs = props.getResourceServer();
    http.securityContextRepository(NoOpServerSecurityContextRepository.getInstance());
    if (rs.isDisableCsrf()) {
      http.csrf(ServerHttpSecurity.CsrfSpec::disable);
    }
    http.cors(cors -> cors.configurationSource(corsConfigurationSource));
    http.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .logout(ServerHttpSecurity.LogoutSpec::disable);

    http.authorizeExchange(spec -> {
      String[] permitAll = rs.getPermitAll();
      if (permitAll != null && permitAll.length > 0) {
        spec.pathMatchers(permitAll).permitAll();
      }
      spec.pathMatchers(HttpMethod.OPTIONS).permitAll();
      spec.anyExchange().authenticated();
    });

    http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
        .jwtDecoder(reactiveJwtDecoder)
        .jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter))));

    return http.build();
  }

  @Bean
  public CorsConfigurationSource gatewayCorsConfigurationSource(SharedSecurityProps props) {
    CorsConfiguration configuration = new CorsConfiguration();
    var allowedOrigins = props.getResourceServer().getAllowedOrigins();
    if (!CollectionUtils.isEmpty(allowedOrigins)) {
      configuration.setAllowedOrigins(allowedOrigins);
    } else {
      configuration.addAllowedOriginPattern("*");
    }
    configuration.addAllowedHeader("*");
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowCredentials(false);
    configuration.setMaxAge(3600L);
    return exchange -> configuration;
  }
}
