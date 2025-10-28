package com.ejada.sec.config;

import com.ejada.starter_security.Role;
import com.ejada.starter_security.SharedSecurityProps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.util.StringUtils;

@Configuration
public class JwtAuthorityConfiguration {

  private static final String DEFAULT_ROLE_PREFIX = "ROLE_";
  private static final String DEFAULT_SCOPE_PREFIX = "SCOPE_";

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter(SharedSecurityProps props) {
    String rolePrefix = resolvePrefix(props.getRolePrefix(), DEFAULT_ROLE_PREFIX);
    String scopePrefix = resolvePrefix(props.getAuthorityPrefix(), DEFAULT_SCOPE_PREFIX);

    JwtGrantedAuthoritiesConverter roleConverter = new JwtGrantedAuthoritiesConverter();
    roleConverter.setAuthorityPrefix(rolePrefix);
    roleConverter.setAuthoritiesClaimName(props.getRolesClaim());

    JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
    scopeConverter.setAuthorityPrefix(scopePrefix);
    scopeConverter.setAuthoritiesClaimName(props.getScopeClaim());

    Set<String> validRoles = EnumSet.allOf(Role.class).stream()
        .map(Enum::name)
        .collect(Collectors.toUnmodifiableSet());

    JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
    authenticationConverter.setJwtGrantedAuthoritiesConverter(
        jwt -> mergeAuthorities(jwt, roleConverter, scopeConverter, validRoles, rolePrefix));
    return authenticationConverter;
  }

  private static Collection<GrantedAuthority> mergeAuthorities(
      Jwt jwt,
      JwtGrantedAuthoritiesConverter roleConverter,
      JwtGrantedAuthoritiesConverter scopeConverter,
      Set<String> validRoles,
      String rolePrefix) {

    List<GrantedAuthority> authorities = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();

    Collection<GrantedAuthority> roleAuthorities = roleConverter.convert(jwt);
    if (roleAuthorities != null) {
      for (GrantedAuthority authority : roleAuthorities) {
        if (authority == null) {
          continue;
        }
        String value = authority.getAuthority();
        if (!StringUtils.hasText(value) || !value.startsWith(rolePrefix)) {
          continue;
        }
        String roleName = value.substring(rolePrefix.length());
        if (!validRoles.contains(roleName)) {
          continue;
        }
        if (seen.add(value)) {
          authorities.add(new SimpleGrantedAuthority(value));
        }
      }
    }

    Collection<GrantedAuthority> scopeAuthorities = scopeConverter.convert(jwt);
    if (scopeAuthorities != null) {
      for (GrantedAuthority authority : scopeAuthorities) {
        if (authority == null) {
          continue;
        }
        String value = authority.getAuthority();
        if (!StringUtils.hasText(value)) {
          continue;
        }
        if (seen.add(value)) {
          authorities.add(new SimpleGrantedAuthority(value));
        }
      }
    }

    return authorities;
  }

  private static String resolvePrefix(String configuredPrefix, String defaultPrefix) {
    return StringUtils.hasText(configuredPrefix) ? configuredPrefix : defaultPrefix;
  }
}
