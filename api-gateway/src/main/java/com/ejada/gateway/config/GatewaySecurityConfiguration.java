package com.ejada.gateway.config;

import com.ejada.gateway.authorization.TenantAuthorizationManager;
import com.ejada.gateway.config.GatewayRateLimitProperties;
import com.ejada.gateway.config.GatewayRoutesProperties;
import com.ejada.gateway.security.ApiKeyAuthenticationFilter;
import com.ejada.gateway.security.GatewaySecurityMetrics;
import com.ejada.gateway.security.GatewayTokenIntrospectionService;
import com.ejada.gateway.security.GatewayTokenRefreshService;
import com.ejada.gateway.security.IpFilteringGatewayFilter;
import com.ejada.gateway.security.JwtRefreshWebFilter;
import com.ejada.gateway.security.RequestSignatureValidationFilter;
import com.ejada.gateway.security.SecurityHeadersFilter;
import com.ejada.gateway.security.UserAgentValidationFilter;
import com.ejada.gateway.security.cors.CorsPreflightCache;
import com.ejada.gateway.security.cors.CorsPreflightCachingFilter;
import com.ejada.gateway.security.apikey.ApiKeyCodec;
import com.ejada.gateway.security.mtls.MutualTlsAuthenticationFilter;
import com.ejada.gateway.security.mtls.PartnerCertificateService;
import com.ejada.gateway.subscription.SubscriptionCacheService;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import com.ejada.starter_security.SharedSecurityProps;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;
import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Reactive security configuration that adapts the shared servlet-based starter
 * to Spring WebFlux.
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewaySecurityConfiguration {

  @Bean
  public ReactiveJwtDecoder reactiveJwtDecoder(JwtDecoder jwtDecoder,
      SharedSecurityProps props,
      ObjectProvider<GatewayTokenIntrospectionService> introspectionServiceProvider) {
    ReactiveJwtDecoder primary = token -> Mono.fromCallable(() -> jwtDecoder.decode(token))
        .subscribeOn(Schedulers.boundedElastic());

    List<ReactiveJwtDecoder> delegates = new ArrayList<>();
    delegates.add(primary);

    ReactiveJwtDecoder oidcDecoder = buildOidcDecoder(props);
    if (oidcDecoder != null) {
      delegates.add(oidcDecoder);
    }

    GatewayTokenIntrospectionService introspectionService = introspectionServiceProvider.getIfAvailable();
    return token -> Mono.defer(() -> tryDecode(delegates, token)
        .flatMap(jwt -> (introspectionService != null)
            ? introspectionService.verify(token, jwt).thenReturn(jwt)
            : Mono.just(jwt)));
  }

  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
      SharedSecurityProps props,
      CorsConfigurationSource corsConfigurationSource,
      ReactiveJwtDecoder reactiveJwtDecoder,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      TenantAuthorizationManager tenantAuthorizationManager) {
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
      spec.pathMatchers("/fallback/**").permitAll();
      spec.pathMatchers(HttpMethod.OPTIONS).permitAll();
      spec.anyExchange().access(tenantAuthorizationManager);
    });

    http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
        .jwtDecoder(reactiveJwtDecoder)
        .jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter))));

    return http.build();
  }

  @Bean
  public TenantAuthorizationManager tenantAuthorizationManager(
      CoreAutoConfiguration.CoreProps coreProps,
      ReactiveStringRedisTemplate redisTemplate,
      SubscriptionCacheService subscriptionCacheService,
      GatewayRateLimitProperties gatewayRateLimitProperties,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper) {
    ObjectMapper mapper = Optional.ofNullable(primaryObjectMapper.getIfAvailable())
        .orElseGet(fallbackObjectMapper::getIfAvailable);
    return new TenantAuthorizationManager(coreProps, redisTemplate, subscriptionCacheService,
        gatewayRateLimitProperties, mapper);
  }

  @Bean
  public GatewaySecurityMetrics gatewaySecurityMetrics(MeterRegistry meterRegistry) {
    return new GatewaySecurityMetrics(meterRegistry);
  }

  @Bean
  public UserAgentValidationFilter userAgentValidationFilter(GatewaySecurityProperties properties) {
    return new UserAgentValidationFilter(properties.getUserAgentValidation());
  }

  @Bean
  public CorsPreflightCache corsPreflightCache(GatewaySecurityProperties properties) {
    return new CorsPreflightCache(properties.getCors().getPreflightCacheTtl());
  }

  @Bean
  public CorsPreflightCachingFilter corsPreflightCachingFilter(
      CorsConfigurationSource corsConfigurationSource,
      CorsPreflightCache corsPreflightCache) {
    return new CorsPreflightCachingFilter(corsConfigurationSource, corsPreflightCache);
  }

  @Bean
  @ConditionalOnClass(ReactiveStringRedisTemplate.class)
  @ConditionalOnBean(ReactiveStringRedisTemplate.class)
  @ConditionalOnProperty(prefix = "gateway.security.api-key", name = "enabled", havingValue = "true", matchIfMissing = true)
  public ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(
      ReactiveStringRedisTemplate redisTemplate,
      GatewaySecurityMetrics metrics,
      GatewaySecurityProperties properties,
      GatewayRoutesProperties routesProperties,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper,
      ApiKeyCodec apiKeyCodec) {
    return new ApiKeyAuthenticationFilter(redisTemplate, metrics, properties, routesProperties, primaryObjectMapper, fallbackObjectMapper, apiKeyCodec);
  }

  @Bean
  @ConditionalOnClass(ReactiveStringRedisTemplate.class)
  @ConditionalOnBean(ReactiveStringRedisTemplate.class)
  @ConditionalOnProperty(prefix = "gateway.security.ip-filtering", name = "enabled", havingValue = "true")
  public IpFilteringGatewayFilter ipFilteringGatewayFilter(
      ReactiveStringRedisTemplate redisTemplate,
      GatewaySecurityProperties properties,
      GatewaySecurityMetrics metrics,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper) {
    return new IpFilteringGatewayFilter(redisTemplate, properties, metrics, primaryObjectMapper, fallbackObjectMapper);
  }

  @Bean
  @ConditionalOnClass(ReactiveStringRedisTemplate.class)
  @ConditionalOnBean(ReactiveStringRedisTemplate.class)
  @ConditionalOnProperty(prefix = "gateway.security.signature-validation", name = "enabled", havingValue = "true")
  public RequestSignatureValidationFilter requestSignatureValidationFilter(
      ReactiveStringRedisTemplate redisTemplate,
      GatewaySecurityProperties properties,
      GatewaySecurityMetrics metrics,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper) {
    return new RequestSignatureValidationFilter(redisTemplate, properties, metrics, primaryObjectMapper, fallbackObjectMapper);
  }

  @Bean
  @ConditionalOnClass({ReactiveStringRedisTemplate.class, WebClient.class})
  @ConditionalOnBean(ReactiveStringRedisTemplate.class)
  @ConditionalOnProperty(prefix = "gateway.security.token-cache", name = "enabled", havingValue = "true")
  public GatewayTokenIntrospectionService gatewayTokenIntrospectionService(
      GatewaySecurityProperties properties,
      ReactiveStringRedisTemplate redisTemplate,
      GatewaySecurityMetrics metrics,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper,
      WebClient.Builder webClientBuilder) {
    return new GatewayTokenIntrospectionService(properties, redisTemplate, metrics, primaryObjectMapper, fallbackObjectMapper, webClientBuilder);
  }

  @Bean
  @ConditionalOnProperty(prefix = "gateway.security.token-refresh", name = "enabled", havingValue = "true")
  public GatewayTokenRefreshService gatewayTokenRefreshService(
      GatewaySecurityProperties properties,
      WebClient.Builder webClientBuilder,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper) {
    return new GatewayTokenRefreshService(properties, webClientBuilder, primaryObjectMapper);
  }

  @Bean
  @ConditionalOnBean(GatewayTokenRefreshService.class)
  public JwtRefreshWebFilter jwtRefreshWebFilter(GatewaySecurityProperties properties,
      GatewayTokenRefreshService tokenRefreshService) {
    return new JwtRefreshWebFilter(properties, tokenRefreshService);
  }

  @Bean
  public SecurityHeadersFilter securityHeadersFilter(GatewaySecurityProperties properties) {
    return new SecurityHeadersFilter(properties);
  }

  @Bean
  @ConditionalOnBean(PartnerCertificateService.class)
  @ConditionalOnProperty(prefix = "gateway.security.mutual-tls", name = "enabled", havingValue = "true")
  public MutualTlsAuthenticationFilter mutualTlsAuthenticationFilter(
      GatewaySecurityProperties properties,
      PartnerCertificateService certificateService,
      GatewaySecurityMetrics metrics,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper) {
    return new MutualTlsAuthenticationFilter(properties, certificateService, metrics, primaryObjectMapper, fallbackObjectMapper);
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

  private Mono<org.springframework.security.oauth2.jwt.Jwt> tryDecode(List<ReactiveJwtDecoder> delegates, String token) {
    return Flux.fromIterable(delegates)
        .concatMap(decoder -> decoder.decode(token).onErrorResume(ex -> Mono.empty()))
        .next()
        .switchIfEmpty(Mono.error(new org.springframework.security.oauth2.jwt.JwtException("Unable to decode JWT")));
  }

  private ReactiveJwtDecoder buildOidcDecoder(SharedSecurityProps props) {
    String jwksUri = props.getJwks() != null ? props.getJwks().getUri() : null;
    if (StringUtils.hasText(jwksUri)) {
      return NimbusReactiveJwtDecoder.withJwkSetUri(jwksUri).build();
    }
    String issuer = props.getIssuer();
    if (StringUtils.hasText(issuer)) {
      JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);
      return token -> Mono.fromCallable(() -> decoder.decode(token))
          .subscribeOn(Schedulers.boundedElastic());
    }
    return null;
  }
}
