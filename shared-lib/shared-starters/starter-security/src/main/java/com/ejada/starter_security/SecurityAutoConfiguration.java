package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.starter_security.web.JsonAccessDeniedHandler;
import com.ejada.starter_security.web.JsonAuthEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Default Resource Server security:
 *  - permit /actuator/health, /v3/api-docs/**, /swagger-ui/** (+ configurable permit-all)
 *  - JWT required for everything else
 *  - JSON 401/403
 *  - Optional tenant propagation from JWT (JwtTenantFilter) when tenant-claim is set
 */
@AutoConfiguration
@EnableConfigurationProperties(SharedSecurityProps.class)
@ConditionalOnClass(SecurityFilterChain.class)
@EnableMethodSecurity
public class SecurityAutoConfiguration {

  private static final String DEFAULT_MODE = "hs256";
  private static final String HS256_ALGORITHM = "HmacSHA256";
  private static final int MIN_HS256_KEY_BYTES = 32;

  private static final List<String> DEFAULT_PERMIT_ALL = List.of(
      "/actuator/health",
      "/v3/api-docs/**",
      "/swagger-ui/**",
      "/swagger-ui.html"
  );

  private static final List<String> DEFAULT_ALLOWED_METHODS = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
  private static final List<String> DEFAULT_ALLOWED_HEADERS = List.of(
      HeaderNames.AUTHORIZATION,
      HeaderNames.CONTENT_TYPE,
      HeaderNames.X_REQUESTED_WITH,
      HeaderNames.CORRELATION_ID,
      HeaderNames.X_TENANT_ID,
      HeaderNames.CSRF_TOKEN
  );
  private static final List<String> DEFAULT_EXPOSED_HEADERS = List.of(
      HeaderNames.CORRELATION_ID,
      HeaderNames.X_TENANT_ID,
      HeaderNames.CSRF_TOKEN
  );

  private static final Set<String> VALID_ROLES = EnumSet.allOf(Role.class).stream()
      .map(Enum::name)
      .collect(Collectors.toUnmodifiableSet());

  /* ---------------------------------------------------
   * RoleChecker : exposes @roleChecker for SpEL usage
   * --------------------------------------------------- */
  @Bean
  @ConditionalOnMissingBean(RoleChecker.class)
  public RoleChecker roleChecker(SharedSecurityProps props) {
    return new RoleChecker(props);
  }

  /* ---------------------------------------------------
   * JwtAuthenticationConverter : roles/scopes mapping
   * --------------------------------------------------- */
  @Bean
  @ConditionalOnMissingBean
  public JwtAuthenticationConverter jwtAuthenticationConverter(SharedSecurityProps props) {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> buildAuthorities(jwt, props));
    return converter;
  }

  private Collection<GrantedAuthority> buildAuthorities(Jwt jwt, SharedSecurityProps props) {
    List<GrantedAuthority> authorities = new ArrayList<>();

    Object rolesObj = claimPath(jwt.getClaims(), props.getRolesClaim());
    addRoleAuthorities(rolesObj, props.getRolePrefix(), authorities);

    String scope = jwt.getClaimAsString(props.getScopeClaim());
    addScopeAuthorities(scope, props.getAuthorityPrefix(), authorities);

    return authorities;
  }

  private void addRoleAuthorities(Object rolesObj, String rolePrefix, List<GrantedAuthority> authorities) {
    if (rolesObj instanceof Collection<?> collection) {
      collection.stream()
          .map(String::valueOf)
          .map(String::trim)
          .filter(role -> !role.isEmpty())
          .filter(VALID_ROLES::contains)
          .map(role -> new SimpleGrantedAuthority(rolePrefix + role))
          .forEach(authorities::add);
      return;
    }

    if (rolesObj instanceof String roles && StringUtils.hasText(roles)) {
      Arrays.stream(roles.split("[,\\s]+"))
          .map(String::trim)
          .filter(role -> !role.isBlank())
          .filter(VALID_ROLES::contains)
          .map(role -> new SimpleGrantedAuthority(rolePrefix + role))
          .forEach(authorities::add);
    }
  }

  private void addScopeAuthorities(String scope, String authorityPrefix, List<GrantedAuthority> authorities) {
    if (!StringUtils.hasText(scope)) {
      return;
    }

    Arrays.stream(scope.split("\\s+"))
        .map(String::trim)
        .filter(sc -> !sc.isBlank())
        .map(sc -> new SimpleGrantedAuthority(authorityPrefix + sc))
        .forEach(authorities::add);
  }

  /* ---------------------------------------------------
   * JwtDecoder : hs256 | jwks | issuer + validators
   * --------------------------------------------------- */
  @Bean
  @ConditionalOnMissingBean(JwtDecoder.class)
  public JwtDecoder jwtDecoder(SharedSecurityProps props) {
    String mode = Optional.ofNullable(props.getMode()).orElse(DEFAULT_MODE).toLowerCase(Locale.ROOT);
    NimbusJwtDecoder decoder = createDecoder(mode, props);

    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(buildValidators(props)));
    return decoder;
  }

  private NimbusJwtDecoder createDecoder(String mode, SharedSecurityProps props) {
    return switch (mode) {
      case "issuer" -> {
        require(StringUtils.hasText(props.getIssuer()), "shared.security.issuer is required when mode=issuer");
        yield NimbusJwtDecoder.withIssuerLocation(props.getIssuer()).build();
      }
      case "jwks" -> {
        String jwksUri = Optional.ofNullable(props.getJwks()).map(SharedSecurityProps.Jwks::getUri).orElse(null);
        require(StringUtils.hasText(jwksUri), "shared.security.jwks.uri is required when mode=jwks");
        yield NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
      }
      case "hs256" -> {
        String secret = Optional.ofNullable(props.getHs256()).map(SharedSecurityProps.Hs256::getSecret).orElse(null);
        require(StringUtils.hasText(secret), "shared.security.hs256.secret is required when mode=hs256");
        SecretKey key = buildHs256Key(secret);
        yield NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
      }
      default -> throw new IllegalArgumentException("Invalid shared.security.mode: " + mode);
    };
  }

  private List<OAuth2TokenValidator<Jwt>> buildValidators(SharedSecurityProps props) {
    List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
    validators.add(JwtValidators.createDefault());
    if (StringUtils.hasText(props.getIssuer())) {
      validators.add(JwtValidators.createDefaultWithIssuer(props.getIssuer()));
    }
    if (StringUtils.hasText(props.getAudience())) {
      validators.add(new JwtClaimValidator<List<String>>(OAuth2ParameterNames.AUDIENCE,
          aud -> aud != null && aud.contains(props.getAudience())));
    }
    return validators;
  }

  private SecretKey buildHs256Key(String secret) {
    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(secret);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
          "shared.security.hs256.secret must be a Base64-encoded string", ex);
    }
    if (decoded.length < MIN_HS256_KEY_BYTES) {
      throw new IllegalArgumentException(
          "shared.security.hs256.secret must decode to at least " + MIN_HS256_KEY_BYTES + " bytes");
    }
    return new SecretKeySpec(decoded, HS256_ALGORITHM);
  }

  /* ---------------------------------------------------
   * SecurityFilterChain : permitAll + JWT for others
   * --------------------------------------------------- */
  @Bean(name = "defaultSecurity")
  @ConditionalOnProperty(prefix = "shared.security.resource-server", name = "enabled", havingValue = "true", matchIfMissing = true)
  @ConditionalOnMissingBean(name = "defaultSecurity")
  public SecurityFilterChain defaultSecurity(HttpSecurity http,
                                             SharedSecurityProps props,
                                             JwtAuthenticationConverter jwtAuthConverter,
                                             ObjectMapper objectMapper,
                                             CorsConfigurationSource corsConfigurationSource) throws Exception {

    SharedSecurityProps.ResourceServer resourceServer = props.getResourceServer();

    applyCsrf(http, resourceServer);
    applySessionManagement(http, resourceServer);

    List<String> permitAllPatterns = buildPermitAll(resourceServer);
    configureAuthorization(http, corsConfigurationSource, permitAllPatterns, jwtAuthConverter, objectMapper);
    addTenantPropagation(http, props);

    return http.build();
  }

  private void applyCsrf(HttpSecurity http, SharedSecurityProps.ResourceServer resourceServer) throws Exception {
    if (resourceServer.isDisableCsrf()) {
      http.csrf(AbstractHttpConfigurer::disable);
    } else {
      http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
      http.addFilterAfter(new CsrfHeaderFilter(), CsrfFilter.class);
    }
  }

  private void applySessionManagement(HttpSecurity http, SharedSecurityProps.ResourceServer resourceServer) throws Exception {
    if (resourceServer.isStateless()) {
      http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
  }

  private void configureAuthorization(HttpSecurity http,
                                      CorsConfigurationSource corsConfigurationSource,
                                      List<String> permitAllPatterns,
                                      JwtAuthenticationConverter jwtAuthConverter,
                                      ObjectMapper objectMapper) throws Exception {

    http.cors(cors -> cors.configurationSource(corsConfigurationSource))
        .authorizeHttpRequests(auth -> {
          auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
          permitAllPatterns.forEach(p -> auth.requestMatchers(p).permitAll());
          auth.anyRequest().authenticated();
        })
        .oauth2ResourceServer(oauth -> oauth
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
        )
        .exceptionHandling(eh -> eh
            .authenticationEntryPoint(new JsonAuthEntryPoint(objectMapper))
            .accessDeniedHandler(new JsonAccessDeniedHandler(objectMapper))
        )
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .headers(headers -> {
          headers.frameOptions(frame -> frame.deny());
          headers.contentTypeOptions(org.springframework.security.config.Customizer.withDefaults());
          headers.httpStrictTransportSecurity(hsts -> hsts
              .maxAgeInSeconds(31536000)
              .includeSubDomains(true)
              .preload(true)
          );
          headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
        });
  }

  private void addTenantPropagation(HttpSecurity http, SharedSecurityProps props) throws Exception {
    if (StringUtils.hasText(props.getTenantClaim())) {
      http.addFilterAfter(new JwtTenantFilter(props.getTenantClaim()), BearerTokenAuthenticationFilter.class);
    }
  }

  @Bean
  @ConditionalOnMissingBean
  public CorsConfigurationSource corsConfigurationSource(SharedSecurityProps props) {
    CorsConfiguration configuration = new CorsConfiguration();
    List<String> origins = props.getResourceServer().getAllowedOrigins();
    if (origins != null && !origins.isEmpty()) {
      configuration.setAllowedOrigins(origins);
    }
    configuration.setAllowedMethods(DEFAULT_ALLOWED_METHODS);
    configuration.setAllowedHeaders(DEFAULT_ALLOWED_HEADERS);
    configuration.setExposedHeaders(DEFAULT_EXPOSED_HEADERS);
    configuration.setAllowCredentials(false);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /* ---------------------------------------------------
   * helpers
   * --------------------------------------------------- */

  private static List<String> buildPermitAll(SharedSecurityProps.ResourceServer rs) {
    LinkedHashSet<String> set = new LinkedHashSet<>(DEFAULT_PERMIT_ALL);
    if (rs.getPermitAll() != null) {
      Collections.addAll(set, rs.getPermitAll());
    }
    return List.copyOf(set);
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }

  /**
   * Supports nested claim resolution via dot path, e.g. "realm_access.roles".
   */
  private static Object claimPath(Map<String, Object> claims, String path) {
    if (!StringUtils.hasText(path)) return null;
    Object cur = claims;
    for (String seg : path.split("\\.")) {
      if (!(cur instanceof Map<?, ?> m)) return null;
      cur = m.get(seg);
      if (cur == null) return null;
    }
    return cur;
  }
}
