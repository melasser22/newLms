package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ejada.starter_security.authorization.AuthorizationExpressions;
import com.ejada.starter_security.web.JsonAccessDeniedHandler;
import com.ejada.starter_security.web.JsonAuthEntryPoint;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;
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
import java.util.stream.Collectors;

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

  /* ---------------------------------------------------
   * RoleChecker : exposes @roleChecker for SpEL usage
   * --------------------------------------------------- */
  @Bean
  @ConditionalOnMissingBean(RoleChecker.class)
  public RoleChecker roleChecker(SharedSecurityProps props) {
    return new RoleChecker(props);
  }

  /* ---------------------------------------------------
   * AuthorizationExpressions : reusable SpEL helpers
   * --------------------------------------------------- */
  @Bean
  @ConditionalOnMissingBean(AuthorizationExpressions.class)
  public AuthorizationExpressions authorizationExpressions(RoleChecker roleChecker) {
    return new AuthorizationExpressions(roleChecker);
  }

  /* ---------------------------------------------------
   * JwtAuthenticationConverter : roles/scopes mapping
   * --------------------------------------------------- */
  @Bean
  @ConditionalOnMissingBean
  public JwtAuthenticationConverter jwtAuthenticationConverter(SharedSecurityProps props) {
    var conv = new JwtAuthenticationConverter();
    // Only allow roles defined in the Role enum
    var validRoles = EnumSet.allOf(Role.class).stream().map(Enum::name).collect(Collectors.toSet());
    conv.setJwtGrantedAuthoritiesConverter(jwt -> {
      List<GrantedAuthority> out = new ArrayList<>();

      // Roles (array or string, supports nested claim path like "realm_access.roles")
      Object rolesObj = claimPath(jwt.getClaims(), props.getRolesClaim());
      if (rolesObj instanceof Collection<?> coll) {
        for (Object r : coll) {
          String role = String.valueOf(r).trim();
          if (!role.isEmpty() && validRoles.contains(role)) {
            out.add(new SimpleGrantedAuthority(props.getRolePrefix() + role));
          }
        }
      } else if (rolesObj instanceof String s && StringUtils.hasText(s)) {
        for (String role : s.split("[,\\s]+")) {
          String trimmed = role.trim();
          if (!trimmed.isBlank() && validRoles.contains(trimmed)) {
            out.add(new SimpleGrantedAuthority(props.getRolePrefix() + trimmed));
          }
        }
      }

      // Scopes (space-delimited string)
      String scope = jwt.getClaimAsString(props.getScopeClaim());
      if (StringUtils.hasText(scope)) {
        for (String sc : scope.split("\\s+")) {
          if (!sc.isBlank()) out.add(new SimpleGrantedAuthority(props.getAuthorityPrefix() + sc.trim()));
        }
      }

      return out;
    });
    return conv;
  }

  /* ---------------------------------------------------
   * JwtDecoder : hs256 | jwks | issuer + validators
   * --------------------------------------------------- */
  @Bean
  @ConditionalOnMissingBean(JwtDecoder.class)
  public JwtDecoder jwtDecoder(SharedSecurityProps props,
                               TenantAwareJwtValidator tenantAwareJwtValidator) {
    String mode = Optional.ofNullable(props.getMode()).orElse("hs256").toLowerCase(Locale.ROOT);
    NimbusJwtDecoder decoder;

    switch (mode) {
      case "issuer" -> {
        require(StringUtils.hasText(props.getIssuer()), "shared.security.issuer is required when mode=issuer");
        decoder = NimbusJwtDecoder.withIssuerLocation(props.getIssuer()).build();
      }
      case "jwks" -> {
        String jwksUri = Optional.ofNullable(props.getJwks()).map(SharedSecurityProps.Jwks::getUri).orElse(null);
        require(StringUtils.hasText(jwksUri), "shared.security.jwks.uri is required when mode=jwks");
        decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
      }
      case "hs256" -> {
        String secret = Optional.ofNullable(props.getHs256()).map(SharedSecurityProps.Hs256::getSecret).orElse(null);
        require(StringUtils.hasText(secret), "shared.security.hs256.secret is required when mode=hs256");
        SecretKey key = new SecretKeySpec(resolveHs256Secret(secret), "HmacSHA256");
        decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
      }
      default -> throw new IllegalArgumentException("Invalid shared.security.mode: " + mode);
    }

    // Validators: timestamp + optional issuer + optional audience
    List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
    validators.add(JwtValidators.createDefault()); // includes timestamp
    if (StringUtils.hasText(props.getIssuer())) {
      validators.add(JwtValidators.createDefaultWithIssuer(props.getIssuer()));
    }
    if (StringUtils.hasText(props.getAudience())) {
      validators.add(new JwtClaimValidator<List<String>>(OAuth2ParameterNames.AUDIENCE,
          aud -> aud != null && aud.contains(props.getAudience())));
    }
    validators.add(tenantAwareJwtValidator);
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
    return decoder;
  }

  @Bean
  @ConditionalOnMissingBean
  public TenantAwareJwtValidator tenantAwareJwtValidator(
      SharedSecurityProps props,
      ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
    return new TenantAwareJwtValidator(props, redisTemplateProvider.getIfAvailable());
  }

  @Bean
  @ConditionalOnMissingBean
  public TenantContextValidatorInterceptor tenantContextValidatorInterceptor(SharedSecurityProps props) {
    return new TenantContextValidatorInterceptor(props);
  }

  @Bean
  @ConditionalOnBean(TenantContextValidatorInterceptor.class)
  public WebMvcConfigurer tenantContextValidatorWebMvcConfigurer(
      TenantContextValidatorInterceptor interceptor) {
    return new WebMvcConfigurer() {
      @Override
      public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).order(Ordered.HIGHEST_PRECEDENCE + 100);
      }
    };
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

    var rs = props.getResourceServer();

    if (rs.isDisableCsrf()) {
      http.csrf(AbstractHttpConfigurer::disable);
    } else {
      http.csrf(csrf -> {
        CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokenRepository.setHeaderName(HeaderNames.CSRF_TOKEN);
        csrf.csrfTokenRepository(tokenRepository);
        String[] ignorePatterns = Optional.ofNullable(rs.getCsrfIgnore()).orElseGet(() -> new String[0]);
        List<RequestMatcher> ignoreMatchers = Arrays.stream(ignorePatterns)
            .filter(StringUtils::hasText)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(SecurityAutoConfiguration::createRequestMatcher)
            .collect(Collectors.toCollection(ArrayList::new));
        if (!ignoreMatchers.isEmpty()) {
          csrf.ignoringRequestMatchers(ignoreMatchers.toArray(new RequestMatcher[0]));
        }
      });
      http.addFilterAfter(new CsrfHeaderFilter(), CsrfFilter.class);
    }
    if (rs.isStateless()) {
      http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }

    // Build a final, de-duplicated list of permitAll patterns
    final List<String> permitAllFinal = buildPermitAll(rs);

    JsonAuthEntryPoint jsonEntryPoint = new JsonAuthEntryPoint(objectMapper);
    http.cors(cors -> cors.configurationSource(corsConfigurationSource))
        .authorizeHttpRequests(auth -> {
          auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
          for (String p : permitAllFinal) {
            auth.requestMatchers(p).permitAll();
          }
          auth.anyRequest().authenticated();
        })
        .oauth2ResourceServer(oauth -> oauth
            .authenticationEntryPoint(jsonEntryPoint)
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
        )
        .exceptionHandling(eh -> eh
            .authenticationEntryPoint(jsonEntryPoint)
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

    // Propagate tenant from JWT claim (after JWT auth), if configured
    if (StringUtils.hasText(props.getTenantClaim())) {
      http.addFilterAfter(new JwtTenantFilter(props.getTenantClaim()), BearerTokenAuthenticationFilter.class);
    }

    return http.build();
  }

  @Bean
  @ConditionalOnMissingBean
  public CorsConfigurationSource corsConfigurationSource(SharedSecurityProps props) {
    CorsConfiguration configuration = new CorsConfiguration();
    List<String> origins = props.getResourceServer().getAllowedOrigins();
    if (origins != null && !origins.isEmpty()) {
      configuration.setAllowedOrigins(origins);
    }
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList(
        HeaderNames.AUTHORIZATION,
        HeaderNames.CONTENT_TYPE,
        "X-Requested-With",
        HeaderNames.CORRELATION_ID,
        HeaderNames.X_TENANT_ID,
        HeaderNames.CSRF_TOKEN));
    configuration.setExposedHeaders(Arrays.asList(
        HeaderNames.CORRELATION_ID,
        HeaderNames.X_TENANT_ID,
        HeaderNames.CSRF_TOKEN));
    boolean csrfEnabled = !props.getResourceServer().isDisableCsrf();
    configuration.setAllowCredentials(csrfEnabled);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /* ---------------------------------------------------
   * helpers
   * --------------------------------------------------- */

  private static List<String> buildPermitAll(SharedSecurityProps.ResourceServer rs) {
    // maintain order & remove duplicates
    LinkedHashSet<String> set = new LinkedHashSet<>();
    set.add("/actuator/health");
    set.add("/v3/api-docs/**");
    set.add("/swagger-ui/**");
    set.add("/swagger-ui.html");
    if (rs.getPermitAll() != null) {
      Collections.addAll(set, rs.getPermitAll());
    }
    return List.copyOf(set);
  }

  private static RequestMatcher createRequestMatcher(String pattern) {
    return new SimpleAntPatternRequestMatcher(pattern);
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }

  private static byte[] resolveHs256Secret(String secret) {
    byte[] decoded = tryDecodeBase64(secret);
    if (decoded != null && decoded.length >= 32) {
      return decoded;
    }
    return secret.getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] tryDecodeBase64(String value) {
    try {
      byte[] decoded = Base64.getDecoder().decode(value);
      return decoded.length == 0 ? null : decoded;
    } catch (IllegalArgumentException ex) {
      return null;
    }
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

  private static final class SimpleAntPatternRequestMatcher implements RequestMatcher {

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
    private static final UrlPathHelper URL_PATH_HELPER = new UrlPathHelper();

    private final String pattern;

    private SimpleAntPatternRequestMatcher(String pattern) {
      this.pattern = pattern;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
      String path = URL_PATH_HELPER.getPathWithinApplication(request);
      return ANT_PATH_MATCHER.match(pattern, path);
    }
  }
}
