package com.ejada.sec.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Ensures unauthenticated access to the authentication endpoints so that
 * CSRF and JWT guards do not block initial login flows.
 */
@Configuration
public class OpenLoginSecurityConfig {

  private static final String[] LOGIN_MATCHERS = {
      "/api/v1/auth/login",
      "/api/v1/auth/register",
      "/api/v1/auth/refresh",
      "/api/v1/auth/logout",
      "/api/v1/auth/forgot-password",
      "/api/v1/auth/reset-password",
      "/api/v1/auth/admin/login",
      "/api/auth/**",
      "/sec/api/v1/auth/**"
  };

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  SecurityFilterChain loginSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher(LOGIN_MATCHERS)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .securityContext(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable);
    return http.build();
  }
}
