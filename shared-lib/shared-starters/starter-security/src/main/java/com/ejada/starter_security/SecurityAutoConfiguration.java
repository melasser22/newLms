package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.starter_security.web.JsonAccessDeniedHandler;
import com.ejada.starter_security.web.JsonAuthEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default Resource Server security using WebFlux.
 */
@AutoConfiguration
@EnableConfigurationProperties(SharedSecurityProps.class)
@ConditionalOnClass(SecurityWebFilterChain.class)
@EnableMethodSecurity
public class SecurityAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(RoleChecker.class)
  public RoleChecker roleChecker(SharedSecurityProps props) {
    return new RoleChecker(props);
  }

  @Bean
  @ConditionalOnMissingBean
  public JwtAuthenticationConverter jwtAuthenticationConverter(SharedSecurityProps props) {
    var conv = new JwtAuthenticationConverter();
    var validRoles = EnumSet.allOf(Role.class).stream().map(Enum::name).collect(Collectors.toSet());
    conv.setJwtGrantedAuthoritiesConverter(jwt -> {
      List<GrantedAuthority> out = new ArrayList<>();

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

  @Bean
  @ConditionalOnMissingBean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                       SharedSecurityProps props,
                                                       JwtAuthenticationConverter jwtAuthConverter,
                                                       ObjectMapper objectMapper,
                                                       CorsConfigurationSource corsConfigurationSource) {
    SharedSecurityProps.ResourceServer rs = props.getResourceServer();

    if (rs.isDisableCsrf()) {
      http.csrf(ServerHttpSecurity.CsrfSpec::disable);
    } else {
      http.csrf(csrf -> csrf.csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse()));
      http.addFilterAfter(new CsrfHeaderFilter(), SecurityWebFiltersOrder.CSRF);
    }

    http.cors(cors -> cors.configurationSource(corsConfigurationSource));

    List<String> permitAll = buildPermitAll(rs);

    http.authorizeExchange(ex -> {
      ex.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll();
      for (String p : permitAll) {
        ex.pathMatchers(p).permitAll();
      }
      ex.anyExchange().authenticated();
    })
        .oauth2ResourceServer(oauth -> oauth
            .jwt(jwt -> jwt.jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(jwtAuthConverter)))
        )
        .exceptionHandling(eh -> eh
            .authenticationEntryPoint(new JsonAuthEntryPoint(objectMapper))
            .accessDeniedHandler(new JsonAccessDeniedHandler(objectMapper))
        )
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .logout(ServerHttpSecurity.LogoutSpec::disable);

    if (StringUtils.hasText(props.getTenantClaim())) {
      http.addFilterAfter(new JwtTenantFilter(props.getTenantClaim()), SecurityWebFiltersOrder.AUTHENTICATION);
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
    configuration.setAllowCredentials(false);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private static List<String> buildPermitAll(SharedSecurityProps.ResourceServer rs) {
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
