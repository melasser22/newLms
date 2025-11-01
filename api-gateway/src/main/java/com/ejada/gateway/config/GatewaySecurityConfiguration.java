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
import com.ejada.starter_security.Role;
import com.ejada.starter_security.SharedSecurityProps;
import com.ejada.starter_security.TenantAwareJwtValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;
import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.security.CsrfTokenResponseWebFilter;

/**
 * Reactive security configuration that adapts the shared servlet-based starter
 * to Spring WebFlux.
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewaySecurityConfiguration {

  @Bean
  @ConditionalOnMissingBean(ReactiveJwtDecoder.class)
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
  @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
  public JwtAuthenticationConverter jwtAuthenticationConverter(SharedSecurityProps props) {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> extractAuthorities(jwt, props));
    return converter;
  }

  @Bean
  @ConditionalOnMissingBean(TenantAwareJwtValidator.class)
  public TenantAwareJwtValidator tenantAwareJwtValidator(
      SharedSecurityProps props,
      ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
    return new TenantAwareJwtValidator(props, redisTemplateProvider.getIfAvailable());
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
    } else {
      CookieServerCsrfTokenRepository tokenRepository = CookieServerCsrfTokenRepository.withHttpOnlyFalse();
      tokenRepository.setHeaderName(HeaderNames.CSRF_TOKEN);
      http.csrf(csrf -> {
        csrf.csrfTokenRepository(tokenRepository);
        csrf.csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler());
        List<ServerWebExchangeMatcher> ignoreMatchers = buildCsrfIgnoreMatchers(rs.getCsrfIgnore());
        if (!ignoreMatchers.isEmpty()) {
          OrServerWebExchangeMatcher ignored = new OrServerWebExchangeMatcher(ignoreMatchers);
          ServerWebExchangeMatcher matcher = new AndServerWebExchangeMatcher(
              CsrfWebFilter.DEFAULT_CSRF_MATCHER,
              new NegatedServerWebExchangeMatcher(ignored));
          csrf.requireCsrfProtectionMatcher(matcher);
        }
      });
      http.addFilterAfter(new CsrfTokenResponseWebFilter(), SecurityWebFiltersOrder.CSRF);
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

  private static Collection<GrantedAuthority> extractAuthorities(Jwt jwt,
      SharedSecurityProps props) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    Set<String> validRoles = EnumSet.allOf(Role.class).stream().map(Enum::name)
        .collect(Collectors.toSet());

    String rolePrefix = StringUtils.hasText(props.getRolePrefix())
        ? props.getRolePrefix()
        : "ROLE_";

    Object rolesObj = claimPath(jwt.getClaims(), props.getRolesClaim());
    if (rolesObj instanceof Collection<?> coll) {
      for (Object value : coll) {
        String role = String.valueOf(value).trim();
        if (!role.isEmpty() && validRoles.contains(role)) {
          authorities.add(new SimpleGrantedAuthority(rolePrefix + role));
        }
      }
    } else if (rolesObj instanceof String str && StringUtils.hasText(str)) {
      for (String value : str.split("[,\\s]+")) {
        String role = value.trim();
        if (!role.isEmpty() && validRoles.contains(role)) {
          authorities.add(new SimpleGrantedAuthority(rolePrefix + role));
        }
      }
    }

    String scope = jwt.getClaimAsString(props.getScopeClaim());
    if (StringUtils.hasText(scope)) {
      for (String token : scope.split("\\s+")) {
        String trimmed = token.trim();
        if (!trimmed.isEmpty()) {
          authorities.add(new SimpleGrantedAuthority(props.getAuthorityPrefix() + trimmed));
        }
      }
    }

    return authorities;
  }

  private static Object claimPath(Map<String, Object> claims, String path) {
    if (!StringUtils.hasText(path)) {
      return null;
    }

    Object current = claims;
    for (String segment : path.split("\\.")) {
      if (!(current instanceof Map<?, ?> map)) {
        return null;
      }
      current = map.get(segment);
      if (current == null) {
        return null;
      }
    }
    return current;
  }

  @Bean
  public TenantAuthorizationManager tenantAuthorizationManager(
      CoreAutoConfiguration.CoreProps coreProps,
      SharedSecurityProps sharedSecurityProps,
      ReactiveStringRedisTemplate redisTemplate,
      SubscriptionCacheService subscriptionCacheService,
      GatewayRateLimitProperties gatewayRateLimitProperties,
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> primaryObjectMapper,
      ObjectProvider<ObjectMapper> fallbackObjectMapper) {
    ObjectMapper mapper = Optional.ofNullable(primaryObjectMapper.getIfAvailable())
        .orElseGet(fallbackObjectMapper::getIfAvailable);
    return new TenantAuthorizationManager(coreProps, sharedSecurityProps, redisTemplate,
        subscriptionCacheService, gatewayRateLimitProperties, mapper);
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
    AtomicReference<Throwable> lastError = new AtomicReference<>();

    return Flux.fromIterable(delegates)
        .concatMap(decoder -> decoder.decode(token)
            .onErrorResume(ex -> {
              lastError.set(ex);
              return Mono.empty();
            }))
        .next()
        .switchIfEmpty(Mono.defer(() -> {
          Throwable error = lastError.get();
          if (error instanceof JwtException jwtException) {
            return Mono.error(jwtException);
          }
          String message = (error != null && StringUtils.hasText(error.getMessage()))
              ? "Unable to decode JWT: " + error.getMessage()
              : "Unable to decode JWT";
          return Mono.error(new JwtException(message, error));
        }));
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

  private static List<ServerWebExchangeMatcher> buildCsrfIgnoreMatchers(String[] patterns) {
    if (patterns == null || patterns.length == 0) {
      return List.of();
    }
    return Arrays.stream(patterns)
        .filter(StringUtils::hasText)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(PathPatternParserServerWebExchangeMatcher::new)
        .collect(Collectors.toList());
  }
}
